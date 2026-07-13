const project = require("../../services/project");
const file = require("../../services/file");
const fieldDraft = require("../../utils/field-draft");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

const DRAFT_NAME = "variation-order";
const EMPTY_FORM = {
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
};

Page({
  data: {
    projects: [],
    projectNames: [],
    sections: [],
    sectionNames: [],
    sectionsLoading: false,
    typeNames: ["设计变更", "现场签证", "工程量增减", "工期影响", "其他"],
    uploadedFiles: [],
    attachedFileIds: [],
    photoCount: 0,
    projectText: "请选择项目",
    sectionText: "请选择施工段",
    editId: "",
    pageTitle: "现场上报",
    editRecord: null,
    form: EMPTY_FORM,
    lookupLoading: false,
    uploading: false,
    uploadText: "可拍摄或从相册选择，最多 6 张",
    saving: false,
    submitting: false,
    draftRestored: false,
    draftCleared: false,
    draftScope: "anonymous",
    variationTypeText: "请选择类型"
  },

  onLoad(query) {
    const editId = query && query.editId ? query.editId : "";
    const editRecord = editId ? wx.getStorageSync("laborflow_edit_variation") : null;
    const user = auth.getUser();
    if (!user || !user.userId) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    const draftScope = String(user.userId);
    const localDraft = editId ? null : fieldDraft.load(DRAFT_NAME, draftScope);
    const restoredRecord = localDraft && localDraft.form ? localDraft.form : null;
    const uploadedFiles = localDraft && Array.isArray(localDraft.uploadedFiles) ? localDraft.uploadedFiles : [];
    const restoredEditId = (localDraft && localDraft.editId) || "";
    this.setData({
      form: Object.assign({}, EMPTY_FORM, restoredRecord || {}, {
        occurrenceDate: (restoredRecord && restoredRecord.occurrenceDate) || format.today()
      }),
      editId: editId || restoredEditId,
      editRecord: editRecord || restoredRecord,
      uploadedFiles,
      attachedFileIds: localDraft && Array.isArray(localDraft.attachedFileIds) ? localDraft.attachedFileIds : [],
      photoCount: uploadedFiles.length,
      draftScope,
      draftRestored: !!localDraft,
      pageTitle: editRecord && editRecord.status === "rejected"
        ? "修改签证"
        : (editRecord || restoredEditId ? "继续上报" : "现场上报")
    });
    this.loadProjects();
  },

  async loadProjects() {
    this.setData({ lookupLoading: true });
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
    } finally {
      this.setData({ lookupLoading: false });
    }
  },

  applyEditRecord(record) {
    const projectItem = this.data.projects.find((item) => String(item.id) === String(record.projectId));
    this.setData({
      form: Object.assign({}, EMPTY_FORM, record, { occurrenceDate: record.occurrenceDate || format.today() }),
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
    this.setData({ ["form." + key]: value, draftCleared: false }, () => this.persistLocalDraft());
  },

  onDateChange(event) {
    this.setData({ "form.occurrenceDate": event.detail.value, draftCleared: false }, () => this.persistLocalDraft());
  },

  onTypeChange(event) {
    const value = this.data.typeNames[Number(event.detail.value)];
    this.setData({ "form.variationType": value, variationTypeText: value, draftCleared: false }, () => this.persistLocalDraft());
  },

  onProjectChange(event) {
    const item = this.data.projects[Number(event.detail.value)];
    this.setData({
      "form.projectId": item.id,
      "form.projectSectionId": "",
      projectText: item.name || "项目 #" + item.id,
      sectionText: "请选择施工段",
      sections: [],
      sectionNames: [],
      draftCleared: false
    }, () => this.persistLocalDraft());
    this.loadSections(item.id);
  },

  async loadSections(projectId) {
    this.setData({ sectionsLoading: true });
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
    } finally {
      this.setData({ sectionsLoading: false });
    }
  },

  onSectionChange(event) {
    const item = this.data.sections[Number(event.detail.value)];
    this.setData({ "form.projectSectionId": item.id, sectionText: item.name || "施工段 #" + item.id, draftCleared: false }, () => this.persistLocalDraft());
  },

  async uploadPhotos() {
    if (this.isBusy()) return;
    const remaining = 6 - this.data.uploadedFiles.length;
    if (remaining <= 0) {
      wx.showToast({ title: "最多可添加 6 张现场照片", icon: "none" });
      return;
    }
    this.setData({ uploading: true, uploadText: "正在选择现场照片..." });
    try {
      const files = await file.chooseAndUpload({
        maxCount: remaining,
        onProgress: ({ current, total, progress }) => {
          this.setData({ uploadText: "正在上传第 " + current + "/" + total + " 张（" + progress + "%）" });
        }
      });
      const uploadedFiles = this.data.uploadedFiles.concat(files.filter((item) => item && item.id));
      this.setData({
        uploadedFiles,
        photoCount: uploadedFiles.length,
        uploadText: uploadedFiles.length ? "已添加 " + uploadedFiles.length + " 张照片" : "未添加照片",
        draftCleared: false
      }, () => this.persistLocalDraft());
    } catch (error) {
      const uploadedFiles = this.data.uploadedFiles.concat((error.uploadedFiles || []).filter((item) => item && item.id));
      this.setData({
        uploadedFiles,
        photoCount: uploadedFiles.length,
        uploadText: uploadedFiles.length ? "部分照片已上传，可继续添加" : "上传未完成，可稍后重试",
        draftCleared: false
      }, () => this.persistLocalDraft());
      wx.showToast({ title: error.message || "上传未完成，可稍后重试", icon: "none" });
    } finally {
      this.setData({ uploading: false });
    }
  },

  removePhoto(event) {
    if (this.isBusy()) return;
    const index = Number(event.currentTarget.dataset.index);
    const removed = this.data.uploadedFiles[index];
    if (removed && this.data.attachedFileIds.some((fileId) => String(fileId) === String(removed.id))) {
      wx.showToast({ title: "该照片已保存到服务器，不能在此页移除", icon: "none" });
      return;
    }
    const uploadedFiles = this.data.uploadedFiles.filter((item, itemIndex) => itemIndex !== index);
    this.setData({
      uploadedFiles,
      photoCount: uploadedFiles.length,
      uploadText: uploadedFiles.length ? "已保留 " + uploadedFiles.length + " 张照片" : "尚未添加现场照片",
      draftCleared: false
    }, () => this.persistLocalDraft());
  },

  validate() {
    const form = this.data.form;
    if (!form.projectId || !form.projectSectionId) return "请选择项目和施工段";
    if (!form.variationType) return "请选择签证类型";
    if (!form.reason || !String(form.reason).trim()) return "请输入发生原因";
    if (!(Number(form.increasedQuantity) > 0)) return "工程量需大于 0";
    return "";
  },

  payload() {
    const form = this.data.form;
    return {
      projectId: form.projectId,
      projectSectionId: form.projectSectionId,
      occurrenceDate: form.occurrenceDate,
      variationType: form.variationType,
      reason: String(form.reason).trim(),
      responsibleParty: form.responsibleParty,
      increasedQuantity: Number(form.increasedQuantity),
      estimatedAmount: Number(form.estimatedAmount || 0),
      laborCount: Number(form.laborCount || 0),
      machineryShift: form.machineryShift,
      materialNote: form.materialNote
    };
  },

  isBusy() {
    return this.data.saving || this.data.submitting || this.data.uploading;
  },

  snapshot() {
    return {
      editId: this.data.editId,
      form: this.data.form,
      uploadedFiles: this.data.uploadedFiles,
      attachedFileIds: this.data.attachedFileIds
    };
  },

  persistLocalDraft() {
    if (this.data.draftCleared) return;
    const hasContent = this.data.editId || this.data.uploadedFiles.length || Object.keys(this.data.form)
      .some((key) => key !== "occurrenceDate" && !!this.data.form[key]);
    if (hasContent) fieldDraft.save(DRAFT_NAME, this.snapshot(), this.data.draftScope);
  },

  clearLocalDraft() {
    fieldDraft.clear(DRAFT_NAME, this.data.draftScope);
    this.setData({ draftCleared: true, draftRestored: false });
  },

  saveLocalDraft() {
    if (this.isBusy()) return;
    this.setData({ draftCleared: false, draftRestored: true }, () => this.persistLocalDraft());
    wx.showToast({ title: "已暂存到本机", icon: "success" });
  },

  discardLocalDraft() {
    if (this.isBusy()) return;
    fieldDraft.clear(DRAFT_NAME, this.data.draftScope);
    this.setData({ draftCleared: true, draftRestored: false }, () => wx.reLaunch({ url: "/pages/variation/create" }));
  },

  async saveRemoteDraft() {
    const saved = this.data.editId
      ? await project.updateVariationOrder(this.data.editId, this.payload())
      : await project.createVariationOrder(this.payload());
    if (!this.data.editId && saved && saved.id) {
      await new Promise((resolve) => this.setData({ editId: saved.id, pageTitle: "编辑草稿" }, resolve));
      this.persistLocalDraft();
    }
    return saved;
  },

  async attachPendingFiles(orderId) {
    if (!orderId) throw new Error("签证保存结果缺少编号");
    for (const uploaded of this.data.uploadedFiles) {
      if (this.data.attachedFileIds.some((fileId) => String(fileId) === String(uploaded.id))) continue;
      await project.attachVariationFile(orderId, uploaded.id);
      await new Promise((resolve) => this.setData({ attachedFileIds: this.data.attachedFileIds.concat(uploaded.id) }, resolve));
      this.persistLocalDraft();
    }
  },

  async saveDraft() {
    await this.persist("draft");
  },

  async submit() {
    await this.persist("submit");
  },

  async persist(action) {
    if (this.isBusy()) return;
    const message = this.validate();
    if (message) {
      wx.showToast({ title: message, icon: "none" });
      return;
    }
    this.setData(action === "submit" ? { submitting: true } : { saving: true });
    try {
      const saved = await this.saveRemoteDraft();
      const orderId = (saved && saved.id) || this.data.editId;
      await this.attachPendingFiles(orderId);
      if (action === "submit") await project.submitVariationOrder(orderId);
      this.clearLocalDraft();
      wx.showToast({ title: action === "submit" ? "签证已提交" : "签证草稿已保存", icon: "success" });
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      this.persistLocalDraft();
      wx.showToast({ title: error.message || "保存失败，已保留本机草稿", icon: "none" });
    } finally {
      this.setData({ saving: false, submitting: false });
    }
  },

  onHide() {
    this.persistLocalDraft();
  },

  onUnload() {
    this.persistLocalDraft();
  }
});
