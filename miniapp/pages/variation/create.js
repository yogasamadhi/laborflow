const project = require("../../services/project");
const file = require("../../services/file");
const format = require("../../utils/format");

Page({
  data: {
    projects: [],
    projectNames: [],
    sections: [],
    sectionNames: [],
    typeNames: ["设计变更", "现场签证", "工程量增减", "工期影响", "其他"],
    fileIds: [],
    photoCount: 0,
    projectText: "请选择项目",
    sectionText: "请选择施工段",
    editId: "",
    pageTitle: "现场上报",
    editRecord: null,
    form: {
      projectId: "",
      projectSectionId: "",
      occurrenceDate: "",
      variationType: "",
      reason: "",
      responsibleParty: "",
      increasedQuantity: "",
      estimatedAmount: "",
      laborCount: "",
      machineryShift: "",
      materialNote: ""
    },
    loading: false,
    variationTypeText: "请选择类型"
  },

  onLoad(query) {
    const editId = query && query.editId ? query.editId : "";
    const editRecord = editId ? wx.getStorageSync("laborflow_edit_variation") : null;
    this.setData({ "form.occurrenceDate": format.today() });
    if (editId && editRecord) {
      this.setData({ editId, editRecord, pageTitle: editRecord.status === "rejected" ? "修改签证" : "编辑草稿" });
    }
    this.loadProjects();
  },

  async loadProjects() {
    try {
      const data = await project.projects();
      const rows = format.pageRecords(data);
      this.setData({
        projects: rows,
        projectNames: rows.map((item) => item.name)
      });
      if (this.data.editRecord) {
        this.applyEditRecord(this.data.editRecord);
      }
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  applyEditRecord(record) {
    const projectItem = this.data.projects.find((item) => String(item.id) === String(record.projectId));
    this.setData({
      form: {
        projectId: record.projectId || "",
        projectSectionId: record.projectSectionId || "",
        occurrenceDate: record.occurrenceDate || format.today(),
        variationType: record.variationType || "",
        reason: record.reason || "",
        responsibleParty: record.responsibleParty || "",
        increasedQuantity: record.increasedQuantity || "",
        estimatedAmount: record.estimatedAmount || "",
        laborCount: record.laborCount || "",
        machineryShift: record.machineryShift || "",
        materialNote: record.materialNote || ""
      },
      projectText: projectItem ? projectItem.name : record.projectId ? "项目 #" + record.projectId : "请选择项目",
      sectionText: record.projectSectionId ? "施工段 #" + record.projectSectionId : "请选择施工段",
      variationTypeText: record.variationType || "请选择类型"
    });
    if (record.projectId) {
      this.loadSections(record.projectId);
    }
  },

  onInput(event) {
    const key = event.currentTarget.dataset.key;
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ ["form." + key]: value });
  },

  onDateChange(event) {
    this.setData({ "form.occurrenceDate": event.detail.value });
  },

  onTypeChange(event) {
    const value = this.data.typeNames[Number(event.detail.value)];
    this.setData({ "form.variationType": value, variationTypeText: value });
  },

  async onProjectChange(event) {
    const item = this.data.projects[Number(event.detail.value)];
    this.setData({
      "form.projectId": item.id,
      "form.projectSectionId": "",
      projectText: item.name || "项目 #" + item.id,
      sectionText: "请选择施工段",
      sections: [],
      sectionNames: []
    });
    this.loadSections(item.id);
  },

  async loadSections(projectId) {
    try {
      const data = await project.sections(projectId);
      const rows = format.pageRecords(data);
      const selected = rows.find((section) => String(section.id) === String(this.data.form.projectSectionId));
      this.setData({
        sections: rows,
        sectionNames: rows.map((section) => section.name),
        sectionText: selected ? selected.name : this.data.sectionText
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  onSectionChange(event) {
    const item = this.data.sections[Number(event.detail.value)];
    this.setData({ "form.projectSectionId": item.id, sectionText: item.name || "施工段 #" + item.id });
  },

  async uploadPhotos() {
    try {
      const files = await file.chooseAndUpload();
      const fileIds = this.data.fileIds.concat(files.map((item) => item.id));
      this.setData({ fileIds, photoCount: fileIds.length });
    } catch (error) {
      wx.showToast({ title: error.message || "取消上传", icon: "none" });
    }
  },

  validate() {
    const form = this.data.form;
    if (!form.projectId || !form.projectSectionId) return "请选择项目和施工段";
    if (!form.variationType) return "请选择签证类型";
    if (!form.reason) return "请输入发生原因";
    if (!form.increasedQuantity) return "请输入工程量";
    return "";
  },

  payload() {
    const form = this.data.form;
    return {
      projectId: form.projectId,
      projectSectionId: form.projectSectionId,
      occurrenceDate: form.occurrenceDate,
      variationType: form.variationType,
      reason: form.reason,
      responsibleParty: form.responsibleParty,
      increasedQuantity: Number(form.increasedQuantity),
      estimatedAmount: Number(form.estimatedAmount || 0),
      laborCount: Number(form.laborCount || 0),
      machineryShift: form.machineryShift,
      materialNote: form.materialNote
    };
  },

  async saveDraft() {
    const message = this.validate();
    if (message) {
      wx.showToast({ title: message, icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      const saved = this.data.editId
        ? await project.updateVariationOrder(this.data.editId, this.payload())
        : await project.createVariationOrder(this.payload());
      for (const fileId of this.data.fileIds) {
        await project.attachVariationFile(saved.id, fileId);
      }
      wx.showToast({ title: this.data.editId ? "已保存修改" : "已保存草稿", icon: "success" });
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  async submit() {
    const message = this.validate();
    if (message) {
      wx.showToast({ title: message, icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      const saved = this.data.editId
        ? await project.updateVariationOrder(this.data.editId, this.payload())
        : await project.createVariationOrder(this.payload());
      for (const fileId of this.data.fileIds) {
        await project.attachVariationFile(saved.id, fileId);
      }
      await project.submitVariationOrder(saved.id);
      wx.showToast({ title: "已提交", icon: "success" });
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  }
});
