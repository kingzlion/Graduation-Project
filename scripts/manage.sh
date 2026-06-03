#!/bin/bash

# ==============================================================================
# 《雄安时空变迁与建设情报平台》一键化控制脚本
# ==============================================================================

# 项目根目录绝对路径定位
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
AGENTS_DIR="$PROJECT_ROOT/agents"
FRONTEND_PC_DIR="$PROJECT_ROOT/frontend-pc"
FRONTEND_H5_DIR="$PROJECT_ROOT/frontend"

# 颜色控制输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 重置颜色

log_info() {
    echo -e "${GREEN}[INFO] $1${NC}"
}

log_warn() {
    echo -e "${YELLOW}[WARN] $1${NC}"
}

log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

# 1. 构建模块
build_all() {
    log_info "正在开始全栈编译..."

    # 构建 PC 前端 SPA 并输出至 Spring Boot 静态资源文件夹
    log_info "步骤 1: 编译前端 PC 页面 (Vite -> Spring Boot)..."
    cd "$FRONTEND_PC_DIR" || exit 1
    npm install
    npm run build
    if [ $? -ne 0 ]; then
        log_error "前端 PC 编译失败，请检查编译日志。"
        exit 1
    fi
    log_info "前端 PC 产物已输出至 backend/src/main/resources/static"

    # 重新打包 Spring Boot 后端
    log_info "步骤 2: 重新打包后端 Spring Boot (包含最新前端资源)..."
    cd "$BACKEND_DIR" || exit 1
    chmod +x mvnw
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        log_error "后端 Spring Boot 打包失败，请检查 Maven 编译日志。"
        exit 1
    fi
    log_info "Spring Boot 后端 Jar 包打包成功。"
}

# 2. 启动模块
start_all() {
    log_info "正在逐个启动各端子系统..."

    # A. 启动 Python AI Agents (Port 8000)
    if lsof -i :8000 >/dev/null 2>&1; then
        log_warn "端口 8000 (AI Agents) 已被占用，跳过启动。"
    else
        log_info "启动 AI Agents 智能体集群 (端口 8000)..."
        cd "$PROJECT_ROOT" || exit 1
        nohup python3 -m uvicorn agents.main:app --port 8000 > "$PROJECT_ROOT/agents.log" 2>&1 &
        sleep 2
    fi

    # B. 启动 Spring Boot 后端 (Port 8080)
    if lsof -i :8080 >/dev/null 2>&1; then
        log_warn "端口 8080 (Spring Boot) 已被占用，跳过启动。"
    else
        log_info "启动 Spring Boot 后端主应用 (端口 8080)..."
        cd "$BACKEND_DIR" || exit 1
        nohup java -jar target/gis-0.0.1-SNAPSHOT.jar > "$PROJECT_ROOT/backend.log" 2>&1 &
        sleep 3
    fi

    # C. 启动 PC 前端开发服务器 (Port 5174)
    if lsof -i :5174 >/dev/null 2>&1; then
        log_warn "端口 5174 (PC Dev Server) 已被占用，跳过启动。"
    else
        log_info "启动 PC 前端开发服务器 (端口 5174)..."
        cd "$FRONTEND_PC_DIR" || exit 1
        nohup npm run dev > "$PROJECT_ROOT/frontend-pc.log" 2>&1 &
        sleep 1
    fi

    # D. 启动移动 H5 前端开发服务器 (Port 5173)
    if lsof -i :5173 >/dev/null 2>&1; then
        log_warn "端口 5173 (H5/小程序 Dev Server) 已被占用，跳过启动。"
    else
        log_info "启动移动 H5 端开发服务器 (端口 5173)..."
        cd "$FRONTEND_H5_DIR" || exit 1
        nohup npm run dev:h5 > "$PROJECT_ROOT/frontend-h5.log" 2>&1 &
        sleep 1
    fi

    log_info "全部服务已尝试在后台挂载启动。"
    status_all
}

# 3. 停止模块
stop_all() {
    log_info "正在停止各端系统进程..."
    
    ports=(8080 8000 5174 5173)
    for port in "${ports[@]}"; do
        pid=$(lsof -t -i :"$port")
        if [ -n "$pid" ]; then
            log_info "杀死占用端口 $port 的进程 (PID: $pid)..."
            kill -9 $pid >/dev/null 2>&1
        else
            log_warn "端口 $port 未被占用，无需停止。"
        fi
    done
    log_info "所有相关进程已停止。"
}

# 4. 状态监测模块
status_all() {
    echo -e "\n=== 运行状态看板 ==="
    
    # 检查 Spring Boot
    if lsof -i :8080 >/dev/null 2>&1; then
        echo -e "🟢 [8080] Spring Boot 后端  -> \033[0;32m运行中\033[0m (http://localhost:8080)"
    else
        echo -e "🔴 [8080] Spring Boot 后端  -> \033[0;31m已停止\033[0m"
    fi

    # 检查 FastAPI Agents
    if lsof -i :8000 >/dev/null 2>&1; then
        echo -e "🟢 [8000] AI Agents 服务   -> \033[0;32m运行中\033[0m (http://127.0.0.1:8000)"
    else
        echo -e "🔴 [8000] AI Agents 服务   -> \033[0;31m已停止\033[0m"
    fi

    # 检查 PC 前端开发
    if lsof -i :5174 >/dev/null 2>&1; then
        echo -e "🟢 [5174] PC 前端开发端    -> \033[0;32m运行中\033[0m (http://localhost:5174)"
    else
        echo -e "🔴 [5174] PC 前端开发端    -> \033[0;31m已停止\033[0m"
    fi

    # 检查 移动 H5 前端开发
    if lsof -i :5173 >/dev/null 2>&1; then
        echo -e "🟢 [5173] 移动 H5 开发端   -> \033[0;32m运行中\033[0m (http://localhost:5173)"
    else
        echo -e "🔴 [5173] 移动 H5 开发端   -> \033[0;31m已停止\033[0m"
    fi
    echo -e "====================\n"
}

# 提示说明
show_help() {
    echo "使用说明:"
    echo "  $0 build   - 一键安装前端依赖并编译前端代码，然后使用 Maven 打包后端 Spring Boot Jar"
    echo "  $0 start   - 启动所有服务 (后台挂载，日志分别输出至根目录各 *.log 中)"
    echo "  $0 stop    - 停止所有服务 (释放 8080, 8000, 5174, 5173 端口)"
    echo "  $0 status  - 查询各端口服务状态"
    echo "  $0 restart - 停止并重启所有服务"
}

case "$1" in
    build)
        build_all
        ;;
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    status)
        status_all
        ;;
    restart)
        stop_all
        sleep 2
        start_all
        ;;
    *)
        show_help
        ;;
esac
