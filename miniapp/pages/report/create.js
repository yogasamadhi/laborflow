const project = require("../../services/project");
const file = require("../../services/file");
const format = require("../../utils/format");

Page({
  data: {
    projects: [],
    projectNames: [],
    sections: [],
    sectionNames: [],
    processes: [],
    processNames: [],
    teams: [],
    teamNames: [],
    fileIds: [],
    photoCount: 0,
    projectText: "请选择项目",
    sectionText: "请选择施工段",
    processText: "请选择工序",
    teamText: "请选择班组",
    editId: "",
    pageTitle: "现场填报",
    editRecord: null,
    form: {
      workDate: "",
      projectId: "",
      projectSectionId: "",
      workProcessId: "",
      completedQuantity: "",
      unit: "",
      teamId: "",
      workerCount: "",
      machineryUsage: "",
      exceptionNote: "",
      reporterName: ""
    },
    loading: false
  },

  onLoad(query) {
    const editId = query && query.editId ? query.editId : "";
    const editRecord = editId ? wx.getStorageSync("laborflow_edit_report") : null;
    this.setData({ "form.workDate": format.today() });
    if (editId && editRecord) {
      this.setData({ editId, editRecord, pageTitle: editRecord.status === "rejected" ? "修改日报" : "编辑草稿" });
    }
    this.loadLookups();
    if (query && query.autoPhoto) {
      setTimeout(() => this.uploadPhotos(), 500);
    }
  },

  async loadLookups() {
    try {
      const [projects, processes, teams] = await Promise.all([
        project.projects(),
        project.workProcesses(),
        project.teams()
      ]);
      const projectRows = format.pageRecords(projects);
      const processRows = format.pageRecords(processes);
      const teamRows = format.pageRecords(teams);
      this.setData({
        projects: projectRows,
        projectNames: projectRows.map((item) => item.name),
        processes: processRows,
        processNames: processRows.map((item) => item.name + (item.unit ? " / " + item.unit : "")),
        teams: teamRows,
        teamNames: teamRows.map((item) => item.name)
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
    const processItem = this.data.processes.find((item) => String(item.id) === String(record.workProcessId));
    const teamItem = this.data.teams.find((item) => String(item.id) === String(record.teamId));
    this.setData({
      form: {
        workDate: record.workDate || format.today(),
        projectId: record.projectId || "",
        projectSectionId: record.projectSectionId || "",
        workProcessId: record.workProcessId || "",
        completedQuantity: record.completedQuantity || "",
        unit: record.unit || "",
        teamId: record.teamId || "",
        workerCount: record.workerCount || "",
        machineryUsage: record.machineryUsage || "",
        exceptionNote: record.exceptionNote || "",
        reporterName: record.reporterName || ""
      },
      projectText: projectItem ? projectItem.name : record.projectId ? "项目 #" + record.projectId : "请选择项目",
      sectionText: record.projectSectionId ? "施工段 #" + record.projectSectionId : "请选择施工段",
      processText: processItem ? processItem.name : record.workProcessId ? "工序 #" + record.workProcessId : "请选择工序",
      teamText: teamItem ? teamItem.name : record.teamId ? "班组 #" + record.teamId : "请选择班组"
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
    this.setData({ "form.workDate": event.detail.value });
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
      const sections = await project.sections(projectId);
      const rows = format.pageRecords(sections);
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

  onProcessChange(event) {
    const item = this.data.processes[Number(event.detail.value)];
    this.setData({
      "form.workProcessId": item.id,
      "form.unit": item.unit || this.data.form.unit,
      processText: item.name || "工序 #" + item.id
    });
  },

  onTeamChange(event) {
    const item = this.data.teams[Number(event.detail.value)];
    this.setData({ "form.teamId": item.id, teamText: item.name || "班组 #" + item.id });
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
    if (!form.projectId || !form.projectSectionId || !form.workProcessId) return "请选择项目、施工段和工序";
    if (!form.completedQuantity) return "请输入完成工程量";
    if (!form.workerCount) return "请输入施工人数";
    return "";
  },

  payload() {
    const form = this.data.form;
    return {
      workDate: form.workDate,
      projectId: form.projectId,
      projectSectionId: form.projectSectionId,
      workProcessId: form.workProcessId,
      completedQuantity: Number(form.completedQuantity),
      unit: form.unit,
      teamId: form.teamId || null,
      workerCount: Number(form.workerCount),
      machineryUsage: form.machineryUsage,
      exceptionNote: form.exceptionNote,
      reporterName: form.reporterName
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
        ? await project.updateDailyReport(this.data.editId, this.payload())
        : await project.createDailyReport(this.payload());
      for (const fileId of this.data.fileIds) {
        await project.attachDailyReportFile(saved.id, fileId);
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
        ? await project.updateDailyReport(this.data.editId, this.payload())
        : await project.createDailyReport(this.payload());
      for (const fileId of this.data.fileIds) {
        await project.attachDailyReportFile(saved.id, fileId);
      }
      await project.submitDailyReport(saved.id);
      wx.showToast({ title: "已提交", icon: "success" });
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  }
});
