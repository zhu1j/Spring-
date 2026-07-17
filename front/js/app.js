/**
 * ============================================================
 * 跨境电商客户资料管理系统 - 主逻辑
 * ============================================================
 * 负责页面渲染、用户交互、表单校验、CRUD 操作。
 */

// ==================== 全局状态 ====================
const STATE = {
    currentPage: 1,
    pageSize: 10,
    filters: {
        keyword: '',
        customerType: '',
        status: '',
        country: '',
    },
    editingId: null,        // 编辑模式下保存当前客户ID
    customerModal: null,    // Bootstrap Modal 实例
    detailModal: null,
    customerTypes: [],      // 客户类型枚举（从后端加载）
};

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    // 初始化 Bootstrap 模态框
    STATE.customerModal = new bootstrap.Modal(document.getElementById('customerModal'));
    STATE.detailModal  = new bootstrap.Modal(document.getElementById('detailModal'));

    // 加载客户类型枚举
    loadCustomerTypes();

    // 首次加载列表
    searchCustomers();
});

// ==================== 客户类型枚举 ====================
async function loadCustomerTypes() {
    try {
        const res = await API.getTypes();
        if (res.code === 200 && res.data) {
            STATE.customerTypes = res.data;
            // 填充筛选下拉框
            fillSelect('filterType', res.data);
            // 填充表单下拉框
            fillSelect('customerType', res.data);
        }
    } catch (e) {
        // 降级：使用硬编码默认值
        const defaults = ['店铺购买', '开店培训', '合作工厂', '物流合作', '其他'];
        STATE.customerTypes = defaults;
        fillSelect('filterType', defaults);
        fillSelect('customerType', defaults);
    }
}

function fillSelect(elementId, options) {
    const select = document.getElementById(elementId);
    if (!select) return;
    // 保留第一项（如"全部"/"请选择"），追加后续选项
    const firstOption = select.options[0];
    select.innerHTML = '';
    if (firstOption && elementId !== 'customerType') {
        select.appendChild(firstOption);
    } else if (elementId === 'customerType') {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = '请选择';
        select.appendChild(opt);
    }
    options.forEach(val => {
        const opt = document.createElement('option');
        opt.value = val;
        opt.textContent = val;
        select.appendChild(opt);
    });
}

// ==================== 列表查询 ====================
async function searchCustomers(pageNum = 1) {
    STATE.currentPage = pageNum;

    // 收集筛选条件
    STATE.filters.keyword      = document.getElementById('filterKeyword').value.trim();
    STATE.filters.customerType = document.getElementById('filterType').value;
    STATE.filters.status       = document.getElementById('filterStatus').value;
    STATE.filters.country      = document.getElementById('filterCountry').value.trim();

    try {
        const res = await API.list({
            pageNum: STATE.currentPage,
            pageSize: STATE.pageSize,
            ...STATE.filters
        });

        if (res.code === 200 && res.data) {
            renderTable(res.data.records);
            renderPagination(res.data.total, res.data.pages, res.data.pageNum);
        } else {
            showToast('查询失败: ' + (res.message || '未知错误'), 'danger');
        }
    } catch (e) {
        showToast(e.message, 'danger');
        // 显示空状态
        document.getElementById('customerTableBody').innerHTML = `
            <tr><td colspan="10" class="text-center text-muted py-5">
                <i class="bi bi-exclamation-triangle" style="font-size: 3rem;"></i>
                <p class="mt-2">${e.message}</p>
            </td></tr>`;
    }
}

// ==================== 表格渲染 ====================
function renderTable(records) {
    const tbody = document.getElementById('customerTableBody');
    document.getElementById('totalCount').textContent = records.length;

    if (!records || records.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="10" class="text-center text-muted py-5">
                <i class="bi bi-inbox" style="font-size: 3rem;"></i>
                <p class="mt-2">暂无匹配的客户数据</p>
            </td></tr>`;
        return;
    }

    tbody.innerHTML = records.map((c, i) => {
        const rowNum = (STATE.currentPage - 1) * STATE.pageSize + i + 1;
        return `
        <tr>
            <td>${rowNum}</td>
            <td>
                <span class="fw-bold text-primary" style="cursor:pointer"
                      onclick="showDetail(${c.id})" title="点击查看详情">
                    ${escapeHtml(c.companyName)}
                </span>
            </td>
            <td>${escapeHtml(c.contactPerson)}</td>
            <td>${escapeHtml(c.phone || '-')}</td>
            <td>${escapeHtml(c.country || '-')}</td>
            <td><span class="type-badge">${escapeHtml(c.customerType)}</span></td>
            <td><span class="status-badge status-${escapeHtml(c.status)}">${escapeHtml(c.status)}</span></td>
            <td>${escapeHtml(c.createdBy || '-')}</td>
            <td>${escapeHtml(c.createdAt || '')}</td>
            <td>
                <button class="btn btn-info btn-sm" onclick="showDetail(${c.id})" title="查看">
                    <i class="bi bi-eye"></i>
                </button>
                <button class="btn btn-warning btn-sm" onclick="showEditModal(${c.id})" title="编辑">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-danger btn-sm" onclick="deleteCustomer(${c.id}, '${escapeHtml(c.companyName)}')" title="删除">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>`;
    }).join('');
}

// ==================== 分页渲染 ====================
function renderPagination(total, totalPages, pageNum) {
    document.getElementById('totalCount').textContent = total;
    document.getElementById('currentPage').textContent = pageNum;
    document.getElementById('totalPages').textContent = totalPages;

    const pagination = document.getElementById('pagination');
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';
    // 上一页
    html += `<li class="page-item ${pageNum <= 1 ? 'disabled' : ''}">
                <a class="page-link" onclick="searchCustomers(${pageNum - 1})">«</a></li>`;

    // 页码按钮（最多显示7个）
    let start = Math.max(1, pageNum - 3);
    let end = Math.min(totalPages, pageNum + 3);
    if (end - start < 6) {
        if (start === 1) end = Math.min(totalPages, start + 6);
        else start = Math.max(1, end - 6);
    }

    for (let i = start; i <= end; i++) {
        html += `<li class="page-item ${i === pageNum ? 'active' : ''}">
                    <a class="page-link" onclick="searchCustomers(${i})">${i}</a></li>`;
    }

    // 下一页
    html += `<li class="page-item ${pageNum >= totalPages ? 'disabled' : ''}">
                <a class="page-link" onclick="searchCustomers(${pageNum + 1})">»</a></li>`;

    pagination.innerHTML = html;
}

// ==================== 重置筛选 ====================
function resetFilter() {
    document.getElementById('filterKeyword').value = '';
    document.getElementById('filterType').value = '';
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterCountry').value = '';
    searchCustomers(1);
}

// ==================== 新增客户 ====================
function showCreateModal() {
    STATE.editingId = null;
    document.getElementById('modalTitle').innerHTML = '<i class="bi bi-person-plus"></i> 新增客户';
    resetForm();
    STATE.customerModal.show();
}

// ==================== 编辑客户 ====================
async function showEditModal(id) {
    try {
        const res = await API.getById(id);
        if (res.code === 200 && res.data) {
            STATE.editingId = id;
            document.getElementById('modalTitle').innerHTML = '<i class="bi bi-pencil"></i> 编辑客户信息';
            fillForm(res.data);
            STATE.customerModal.show();
        } else {
            showToast('获取客户信息失败', 'danger');
        }
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

// ==================== 查看详情 ====================
async function showDetail(id) {
    try {
        const res = await API.getById(id);
        if (res.code === 200 && res.data) {
            const c = res.data;
            document.getElementById('detailContent').innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <div class="detail-label">公司名称</div>
                        <div class="detail-value">${escapeHtml(c.companyName)}</div>

                        <div class="detail-label">联系人</div>
                        <div class="detail-value">${escapeHtml(c.contactPerson)}</div>

                        <div class="detail-label">联系电话</div>
                        <div class="detail-value">${escapeHtml(c.phone || '-')}</div>

                        <div class="detail-label">邮箱</div>
                        <div class="detail-value">${escapeHtml(c.email || '-')}</div>

                        <div class="detail-label">微信号</div>
                        <div class="detail-value">${escapeHtml(c.wechat || '-')}</div>

                        <div class="detail-label">国家/地区</div>
                        <div class="detail-value">${escapeHtml(c.country || '-')}</div>

                        <div class="detail-label">网站</div>
                        <div class="detail-value">${c.website ? `<a href="${escapeHtml(c.website)}" target="_blank">${escapeHtml(c.website)}</a>` : '-'}</div>
                    </div>
                    <div class="col-md-6">
                        <div class="detail-label">客户类型</div>
                        <div class="detail-value"><span class="type-badge">${escapeHtml(c.customerType)}</span></div>

                        <div class="detail-label">跟进状态</div>
                        <div class="detail-value"><span class="status-badge status-${escapeHtml(c.status)}">${escapeHtml(c.status)}</span></div>

                        <div class="detail-label">客户来源</div>
                        <div class="detail-value">${escapeHtml(c.source || '-')}</div>

                        <div class="detail-label">地址</div>
                        <div class="detail-value">${escapeHtml(c.address || '-')}</div>

                        <div class="detail-label">录入人</div>
                        <div class="detail-value">${escapeHtml(c.createdBy || '-')}</div>

                        <div class="detail-label">创建时间</div>
                        <div class="detail-value">${escapeHtml(c.createdAt || '')}</div>

                        <div class="detail-label">更新时间</div>
                        <div class="detail-value">${escapeHtml(c.updatedAt || '')}</div>
                    </div>
                    <div class="col-12 mt-2">
                        <div class="detail-label">服务需求</div>
                        <div class="detail-value" style="white-space:pre-wrap;">${escapeHtml(c.serviceNeeds || '-')}</div>

                        <div class="detail-label">备注</div>
                        <div class="detail-value" style="white-space:pre-wrap;">${escapeHtml(c.remark || '-')}</div>
                    </div>
                </div>`;
            STATE.detailModal.show();
        }
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

// ==================== 提交表单 ====================
async function submitForm() {
    // 前端基础校验
    const companyName = document.getElementById('companyName').value.trim();
    const contactPerson = document.getElementById('contactPerson').value.trim();
    const customerType = document.getElementById('customerType').value;

    if (!companyName) { showToast('请输入公司名称', 'warning'); return; }
    if (!contactPerson) { showToast('请输入联系人', 'warning'); return; }
    if (!customerType) { showToast('请选择客户类型', 'warning'); return; }

    // 组装数据
    const data = {
        companyName: companyName,
        contactPerson: contactPerson,
        phone: document.getElementById('phone').value.trim(),
        email: document.getElementById('email').value.trim(),
        wechat: document.getElementById('wechat').value.trim(),
        country: document.getElementById('country').value.trim(),
        address: document.getElementById('address').value.trim(),
        website: document.getElementById('website').value.trim(),
        customerType: customerType,
        status: document.getElementById('status').value,
        source: document.getElementById('source').value.trim(),
        serviceNeeds: document.getElementById('serviceNeeds').value.trim(),
        remark: document.getElementById('remark').value.trim(),
        createdBy: document.getElementById('createdBy').value.trim(),
    };

    try {
        let res;
        if (STATE.editingId) {
            res = await API.update(STATE.editingId, data);
        } else {
            res = await API.create(data);
        }

        if (res.code === 200) {
            STATE.customerModal.hide();
            showToast(res.message || '操作成功', 'success');
            searchCustomers(STATE.editingId ? STATE.currentPage : 1); // 新增跳第1页，编辑留在当前页
        } else {
            showToast(res.message || '操作失败', 'danger');
        }
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

// ==================== 删除客户 ====================
function deleteCustomer(id, name) {
    if (!confirm(`确定要删除客户【${name}】吗？\n\n此操作为逻辑删除，管理员可恢复。`)) {
        return;
    }
    API.delete(id)
        .then(res => {
            if (res.code === 200) {
                showToast('删除成功', 'success');
                searchCustomers(STATE.currentPage);
            } else {
                showToast(res.message || '删除失败', 'danger');
            }
        })
        .catch(e => showToast(e.message, 'danger'));
}

// ==================== 导出 Excel ====================
function exportExcel() {
    showToast('正在生成Excel文件，请稍候...', 'info');
    API.exportExcel(STATE.filters);
}

// ==================== 共享浏览 ====================
function openSharePage() {
    window.open('pages/share.html', '_blank');
}

// ==================== 分页条数切换 ====================
function changePageSize() {
    STATE.pageSize = parseInt(document.getElementById('pageSizeSelect').value);
    searchCustomers(1);
}

// ==================== 辅助函数 ====================

/** 填充表单（编辑模式） */
function fillForm(c) {
    document.getElementById('customerId').value = c.id;
    document.getElementById('companyName').value = c.companyName || '';
    document.getElementById('contactPerson').value = c.contactPerson || '';
    document.getElementById('phone').value = c.phone || '';
    document.getElementById('email').value = c.email || '';
    document.getElementById('wechat').value = c.wechat || '';
    document.getElementById('country').value = c.country || '';
    document.getElementById('address').value = c.address || '';
    document.getElementById('website').value = c.website || '';
    document.getElementById('customerType').value = c.customerType || '';
    document.getElementById('status').value = c.status || '潜在客户';
    document.getElementById('source').value = c.source || '';
    document.getElementById('serviceNeeds').value = c.serviceNeeds || '';
    document.getElementById('remark').value = c.remark || '';
    document.getElementById('createdBy').value = c.createdBy || '';
}

/** 重置表单 */
function resetForm() {
    document.getElementById('customerForm').reset();
    document.getElementById('customerId').value = '';
    document.getElementById('status').value = '潜在客户';
}

/** XSS 防护：转义 HTML 特殊字符 */
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

/** Toast 消息提示 */
function showToast(message, type = 'info') {
    // 创建 toast 容器（如果不存在）
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'position-fixed top-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    const icons = { success: 'check-circle', danger: 'x-circle', warning: 'exclamation-triangle', info: 'info-circle' };
    const icon = icons[type] || 'info-circle';

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-bg-${type} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body"><i class="bi bi-${icon} me-2"></i>${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>`;

    container.appendChild(toastEl);

    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    // 自动移除DOM
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}
