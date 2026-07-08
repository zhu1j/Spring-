/**
 * ============================================================
 * API 请求封装层
 * ============================================================
 * 统一管理所有对后端接口的 HTTP 请求。
 * 如果后端部署在其他地址，修改 BASE_URL 即可。
 */

const API = (() => {
        // ==================== 配置 ====================

        /** 后端API基础地址（开发环境） */
        const BASE_URL = 'http://localhost:8080';

        /** 请求超时时间（毫秒） */
        const TIMEOUT = 15000;

            // ==================== 核心请求方法 ====================

    /**
     * 发起 HTTP 请求
     *
     * @param {string} url     - 接口路径（相对 BASE_URL）
     * @param {string} method  - HTTP 方法
     * @param {object} data    - 请求体（GET请求忽略）
     * @returns {Promise}      - 解析后的响应数据
     */
    async function request(url,method = 'GET' , data = null) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TIMEOUT);

        const options = {
            method: method,
            signal: controller.signal,
            headers: {
                'Content-Type':'application/json',
            },
        };

        //GET 请求不带 body
        if (data && method !== 'GET') {
            options.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(BASE_URL + url, options);
            clearTimeout(timeoutId);

            if(!response.ok) {
                // HTTP 错误的状态码 (非200-299)
                const errorBody = await response.text();
                let errorMsg;
                try {
                    const errJson = JSON.parse(errorBody);
                    errorMsg = errJson.message || `HTTP ${response.status}`;
                } catch (e) {
                    errorMsg = `请求失败 (${response.status})`;
                }
                throw new Error(errorMsg);
            }

            const result = await response.json();
            return result;
        } catch (error) {
            clearTimeout(timeoutId);

            if (error.name === 'AbortError') {
                throw new Error('请求超时，请检查网络连接');
            }
            if (error.message === 'Failed to fetch') {
                throw new Error('无法连接服务器，请确认后端已启动');
            }
            throw error;
        }
    }

    // ==================== 客户 API ====================
    return {
        /**
         * 新增客户
         * @param {object} customer - 客户信息对象
         */
        create: (customer) => request('/api/customers', 'POST', customer),

        /**
         * 更新客户
         * @param {number} id       - 客户ID
         * @param {object} customer - 更新的字段
         */
        update: (id, customer) => request(`/api/customers/${id}`, 'PUT', customer),

        /**
         * 删除客户（逻辑删除）
         * @param {number} id - 客户ID
         */
        delete: (id) => request(`/api/customers/${id}`, 'DELETE'),

        /**
         * 查询单个客户
         * @param {number} id - 客户ID
         */
        getById: (id) => request(`/api/customers/${id}`),

        /**
         * 分页+条件查询客户列表
         * @param {object} query - 查询条件 {pageNum, pageSize, keyword, customerType, status, country}
         */
        list: (query) => request('/api/customers/list', 'POST', query),

        /**
         * 导出 Excel（直接下载文件）
         * @param {object} filters - 筛选条件
         */
        exportExcel: (filters = {}) => {
            const params = new URLSearchParams();
            if (filters.customerType) params.append('customerType', filters.customerType);
            if (filters.status) params.append('status', filters.status);
            if (filters.country) params.append('country', filters.country);
            if (filters.keyword) params.append('keyword', filters.keyword);

            const url = `${BASE_URL}/api/customers/export?${params.toString()}`;
            // 直接打开下载链接（浏览器会自动下载）
            window.open(url, '_blank');
        },

        /**
         * 共享浏览客户列表
         * @param {object} query - 查询条件
         */
        shareList: (query) => request('/api/customers/share', 'POST', query),

        /**
         * 获取客户类型枚举
         */
        getTypes: () => request('/api/customers/types'),
    };
})();