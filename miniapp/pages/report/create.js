const project = require("../../services/project");
const file = require("../../services/file");
const fieldDraft = require("../../utils/field-draft");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

const DRAFT_NAME = "daily-report";
const EMPTY_FORM = {
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
};

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
    uploadedFiles: [],
    attachedFileIds: [],
    photoCount: 0,
    projectText: "请选择项目",
    sectionText: "请选择施工段",
    processText: "请选择工序",
    teamText: "请选择班组",
    editId: "",
    pageTitle: "现场填报",
    editRecord: null,
    form: EMPTY_FORM,
    lookupLoading: false,
    uploading: false,
    uploadText: "可拍摄或从相册选择，最多 6 张",
    saving: false,
    submitting: false,
    draftRestored: false,
    draftCleared: false,
    draftScope: "anonymous"
  },

  onLoad(query) {
    const editId = query && query.editId ? query.editId : "";
    const editRecord = editId ? wx.getStorageSync("laborflow_edit_report") : null;
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
        workDate: (restoredRecord && restoredRecord.workDate) || format.today()
      }),
      editId: editId || restoredEditId,
      editRecord: editRecord || restoredRecord,
      uploadedFiles,
      attachedFileIds: localDraft && Array.isArray(localDraft.attachedFileIds) ? localDraft.attachedFileIds : [],
      photoCount: uploadedFiles.length,
      draftScope,
      draftRestored: !!localDraft,
      pageTitle: editRecord && editRecord.status === "rejected"
        ? "修改日报"
        : (editRecord || restoredEditId ? "继续填报" : "现场填报")
    });
    this.loadLookups();
    if (query && query.autoPhoto) {
      setTimeout(() => this.uploadPhotos(), 500);
    }
  },

  async loadLookups() {
    this.setData({ lookupLoading: true });
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
    } finally {
      this.setData({ lookupLoading: false });
    }
  },

  applyEditRecord(record) {
    const projectItem = this.data.projects.find((item) => String(item.id) === String(record.projectId));
    const processItem = this.data.processes.find((item) => String(item.id) === String(record.workProcessId));
    const teamItem = this.data.teams.find((item) => String(item.id) === String(record.teamId));
    this.setData({
      form: Object.assign({}, EMPTY_FORM, record, { workDate: record.workDate || format.today() }),
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
    this.setData({ ["form." + key]: value, draftCleared: false }, () => this.persistLocalDraft());
  },

  onDateChange(event) {
    this.setData({ "form.workDate": event.detail.value, draftCleared: false }, () => this.persistLocalDraft());
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
    this.setData({ "form.projectSectionId": item.id, sectionText: item.name || "施工段 #" + item.id, draftCleared: false }, () => this.persistLocalDraft());
  },

  onProcessChange(event) {
    const item = this.data.processes[Number(event.detail.value)];
    this.setData({
      "form.workProcessId": item.id,
      "form.unit": item.unit || this.data.form.unit,
      processText: item.name || "工序 #" + item.id,
      draftCleared: false
    }, () => this.persistLocalDraft());
  },

  onTeamChange(event) {
    const item = this.data.teams[Number(event.detail.value)];
    this.setData({ "form.teamId": item.id, teamText: item.name || "班组 #" + item.id, draftCleared: false }, () => this.persistLocalDraft());
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
    if (!form.projectId || !form.projectSectionId || !form.workProcessId) return "请选择项目、施工段和工序";
    if (!(Number(form.completedQuantity) > 0)) return "完成工程量需大于 0";
    if (!form.unit || !String(form.unit).trim()) return "请输入工程量单位";
    if (!(Number(form.workerCount) > 0)) return "施工人数需大于 0";
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
      unit: String(form.unit).trim(),
      teamId: form.teamId || null,
      workerCount: Number(form.workerCount),
      machineryUsage: form.machineryUsage,
      exceptionNote: form.exceptionNote,
      reporterName: form.reporterName
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
      .some((key) => key !== "workDate" && !!this.data.form[key]);
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
    this.setData({ draftCleared: true, draftRestored: false }, () => wx.reLaunch({ url: "/pages/report/create" }));
  },

  async saveRemoteDraft() {
    const saved = this.data.editId
      ? await project.updateDailyReport(this.data.editId, this.payload())
      : await project.createDailyReport(this.payload());
    if (!this.data.editId && saved && saved.id) {
      await new Promise((resolve) => this.setData({ editId: saved.id, pageTitle: "编辑草稿" }, resolve));
      this.persistLocalDraft();
    }
    return saved;
  },

  async attachPendingFiles(reportId) {
    if (!reportId) throw new Error("日报保存结果缺少编号");
    for (const uploaded of this.data.uploadedFiles) {
      if (this.data.attachedFileIds.some((fileId) => String(fileId) === String(uploaded.id))) continue;
      await project.attachDailyReportFile(reportId, uploaded.id);
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
      const reportId = (saved && saved.id) || this.data.editId;
      await this.attachPendingFiles(reportId);
      if (action === "submit") await project.submitDailyReport(reportId);
      this.clearLocalDraft();
      wx.showToast({ title: action === "submit" ? "日报已提交" : "日报草稿已保存", icon: "success" });
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
