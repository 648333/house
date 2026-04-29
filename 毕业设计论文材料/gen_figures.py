"""
生成论文所需全部图表（架构图、ER图、类图、时序图、活动图）
输出到 figures_generated/ 目录
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import matplotlib.patheffects as pe

matplotlib.rcParams['font.family'] = ['Microsoft YaHei', 'SimHei', 'Arial Unicode MS', 'DejaVu Sans']
matplotlib.rcParams['axes.unicode_minus'] = False

OUT = r"E:\AI\house\project\daojishi\毕业设计论文材料\figures_generated"
os.makedirs(OUT, exist_ok=True)

# ─── 颜色方案 ────────────────────────────────────────────────────────────────
C_BLUE   = "#4A90D9"
C_GREEN  = "#5CB85C"
C_ORANGE = "#F0AD4E"
C_RED    = "#D9534F"
C_PURPLE = "#9B59B6"
C_GRAY   = "#95A5A6"
C_DARK   = "#2C3E50"
C_LIGHT  = "#ECF0F1"
C_WHITE  = "#FFFFFF"

def box(ax, x, y, w, h, text, fc=C_BLUE, tc="white", fs=9, bold=False, radius=0.3):
    rect = FancyBboxPatch((x-w/2, y-h/2), w, h,
                          boxstyle=f"round,pad=0.05,rounding_size={radius}",
                          facecolor=fc, edgecolor="white", linewidth=1.5, zorder=3)
    ax.add_patch(rect)
    ax.text(x, y, text, ha='center', va='center', fontsize=fs,
            color=tc, fontweight='bold' if bold else 'normal', zorder=4, wrap=True)

def arrow(ax, x1, y1, x2, y2, color=C_DARK, lw=1.5, style='->', label=''):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color, lw=lw), zorder=2)
    if label:
        mx, my = (x1+x2)/2, (y1+y2)/2
        ax.text(mx, my, label, fontsize=7, color=color, ha='center', va='bottom', zorder=5)

def line(ax, x1, y1, x2, y2, color=C_GRAY, lw=1, ls='-'):
    ax.plot([x1,x2],[y1,y2], color=color, lw=lw, ls=ls, zorder=1)

# ════════════════════════════════════════════════════════════════════════
# 图1：系统总体架构图
# ════════════════════════════════════════════════════════════════════════
def fig_architecture():
    fig, ax = plt.subplots(figsize=(14, 9))
    ax.set_xlim(0, 14); ax.set_ylim(0, 9)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.set_facecolor('#F8FAFC')

    # 标题
    ax.text(7, 8.6, '基于 Spring Boot 的房屋交易平台系统架构图',
            ha='center', va='center', fontsize=13, fontweight='bold', color=C_DARK)

    # 层标签
    def layer_label(y, text, color):
        rect = FancyBboxPatch((0.1, y-0.35), 1.1, 0.7,
                              boxstyle="round,pad=0.05", facecolor=color, alpha=0.15,
                              edgecolor=color, linewidth=1, zorder=1)
        ax.add_patch(rect)
        ax.text(0.65, y, text, ha='center', va='center', fontsize=8,
                color=color, fontweight='bold')

    layer_label(7.5, '用户层', C_BLUE)
    layer_label(6.0, '前端层', C_GREEN)
    layer_label(4.5, '接口层', C_ORANGE)
    layer_label(3.0, '业务层', C_RED)
    layer_label(1.5, '数据层', C_PURPLE)

    # 用户层
    for i, (lbl, x) in enumerate([('普通用户\n浏览器', 3.5), ('经纪人\n浏览器', 7), ('管理员\n浏览器', 10.5)]):
        box(ax, x, 7.5, 2.2, 0.7, lbl, fc=C_BLUE, fs=9)

    # 前端层
    box(ax, 3.5, 6.0, 2.2, 0.7, 'Vue3 + Vite\n用户前台', fc=C_GREEN, fs=8.5)
    box(ax, 7.0, 6.0, 2.2, 0.7, 'Vue3 + Vite\n经纪人工作台', fc=C_GREEN, fs=8.5)
    box(ax, 10.5, 6.0, 2.2, 0.7, 'Vue3 + Vite\n管理员后台', fc=C_GREEN, fs=8.5)
    box(ax, 7.0, 5.2, 5.5, 0.55, 'Element Plus  |  ECharts  |  Leaflet  |  model-viewer', fc='#27AE60', fs=8)

    # 接口层
    box(ax, 7.0, 4.5, 12.0, 0.65, 'Spring Boot REST API  (AuthController / PropertyController / AppointmentController / PaymentController / StatsController …)', fc=C_ORANGE, fs=8)

    # 业务层
    modules = [
        ('用户认证\nSpring Security\nJWT', 2.0),
        ('房源管理\n发布/审核/检索', 4.2),
        ('预约看房\n时间冲突校验', 6.4),
        ('支付订单\n状态机流转', 8.6),
        ('推荐服务\n规则+模型融合', 10.8),
        ('统计分析\nECharts看板', 12.8),
    ]
    for lbl, x in modules:
        box(ax, x, 3.0, 1.9, 0.85, lbl, fc=C_RED, fs=7.5)

    # 数据层
    box(ax, 3.5, 1.5, 3.0, 0.65, 'MySQL 8\n业务数据存储', fc=C_PURPLE, fs=8.5)
    box(ax, 7.0, 1.5, 2.8, 0.65, 'Spring Data JPA\n持久层访问', fc=C_PURPLE, fs=8.5)
    box(ax, 10.5, 1.5, 3.0, 0.65, '本地文件存储\n图片/模型上传', fc=C_PURPLE, fs=8.5)

    # 层间连接线
    for x in [3.5, 7.0, 10.5]:
        line(ax, x, 7.15, x, 6.35, lw=1.2)
    for x in [3.5, 7.0, 10.5]:
        line(ax, x, 5.65, x, 4.83, lw=1.2)
    line(ax, 7.0, 4.18, 7.0, 3.43, lw=1.2)
    for x in [2.0, 4.2, 6.4, 8.6, 10.8, 12.8]:
        line(ax, x, 2.58, x, 1.83, lw=1.0)

    plt.tight_layout(pad=0.3)
    path = os.path.join(OUT, 'fig4-1-architecture.png')
    plt.savefig(path, dpi=180, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close()
    print(f"  ✔ 架构图: {path}")


# ════════════════════════════════════════════════════════════════════════
# 图2：数据库 ER 图
# ════════════════════════════════════════════════════════════════════════
def fig_er():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')

    ax.text(8, 9.6, '数据库实体关系图（ER图）',
            ha='center', va='center', fontsize=13, fontweight='bold', color=C_DARK)

    def entity(ax, x, y, title, fields, w=2.6, fc=C_BLUE):
        row_h = 0.32
        total_h = row_h * (len(fields) + 1) + 0.1
        # header
        rect = FancyBboxPatch((x-w/2, y-row_h/2), w, row_h,
                              boxstyle="round,pad=0.02", facecolor=fc, edgecolor='#BDC3C7', lw=1, zorder=3)
        ax.add_patch(rect)
        ax.text(x, y, title, ha='center', va='center', fontsize=8.5,
                color='white', fontweight='bold', zorder=4)
        # fields
        for i, (pk, fname, ftype) in enumerate(fields):
            fy = y - row_h*(i+1)
            bg = '#FAFAFA' if i % 2 == 0 else '#F0F4F8'
            rect2 = FancyBboxPatch((x-w/2, fy-row_h/2), w, row_h,
                                   boxstyle="round,pad=0.01", facecolor=bg,
                                   edgecolor='#BDC3C7', lw=0.5, zorder=3)
            ax.add_patch(rect2)
            prefix = '🔑 ' if pk == 'PK' else ('FK ' if pk == 'FK' else '   ')
            ax.text(x-w/2+0.1, fy, f"{prefix}{fname}", ha='left', va='center',
                    fontsize=7, color=C_DARK, zorder=4)
            ax.text(x+w/2-0.1, fy, ftype, ha='right', va='center',
                    fontsize=6.5, color=C_GRAY, zorder=4)
        return y - row_h*(len(fields)+1)  # bottom y

    # users
    entity(ax, 2.2, 9.0, 'users', [
        ('PK','id','Long'),
        ('','username','String'),
        ('','password','String'),
        ('','email','String'),
        ('','role','ENUM'),
        ('','enabled','Boolean'),
    ], fc=C_BLUE)

    # properties
    entity(ax, 6.5, 9.0, 'properties', [
        ('PK','id','Long'),
        ('FK','owner_id','Long'),
        ('','title','String'),
        ('','price','Decimal'),
        ('','address','String'),
        ('','area','Double'),
        ('','layout','String'),
        ('','status','ENUM'),
        ('','tags','String'),
    ], fc=C_GREEN)

    # appointments
    entity(ax, 10.8, 9.0, 'appointments', [
        ('PK','id','Long'),
        ('FK','user_id','Long'),
        ('FK','property_id','Long'),
        ('','appointment_time','DateTime'),
        ('','status','ENUM'),
        ('','message','String'),
    ], fc=C_ORANGE)

    # payment_orders
    entity(ax, 14.2, 9.0, 'payment_orders', [
        ('PK','id','Long'),
        ('FK','payer_id','Long'),
        ('FK','property_id','Long'),
        ('FK','appointment_id','Long'),
        ('','amount','Decimal'),
        ('','status','ENUM'),
        ('','out_trade_no','String'),
    ], fc=C_RED)

    # messages
    entity(ax, 2.2, 4.8, 'messages', [
        ('PK','id','Long'),
        ('FK','sender_id','Long'),
        ('FK','receiver_id','Long'),
        ('FK','property_id','Long'),
        ('','content','Text'),
        ('','is_read','Boolean'),
    ], fc=C_PURPLE)

    # reviews
    entity(ax, 6.0, 4.8, 'reviews', [
        ('PK','id','Long'),
        ('FK','user_id','Long'),
        ('FK','property_id','Long'),
        ('','rating','Integer'),
        ('','content','Text'),
    ], fc='#1ABC9C')

    # support_tickets
    entity(ax, 10.0, 4.8, 'support_tickets', [
        ('PK','id','Long'),
        ('FK','user_id','Long'),
        ('FK','property_id','Long'),
        ('','category','String'),
        ('','status','ENUM'),
        ('','priority','ENUM'),
    ], fc='#E67E22')

    # house_requirements
    entity(ax, 13.8, 4.8, 'house_requirements', [
        ('PK','id','Long'),
        ('FK','user_id','Long'),
        ('','min_price','Decimal'),
        ('','max_price','Decimal'),
        ('','preferred_area','String'),
        ('','layout','String'),
    ], fc='#8E44AD')

    # 关系连线
    def rel(x1,y1,x2,y2,label=''):
        ax.annotate('', xy=(x2,y2), xytext=(x1,y1),
                    arrowprops=dict(arrowstyle='<->', color=C_GRAY, lw=1.2))
        if label:
            ax.text((x1+x2)/2, (y1+y2)/2+0.12, label, fontsize=6.5, color=C_GRAY, ha='center')

    # users → properties (1:N)
    rel(3.5, 8.1, 5.2, 8.1, '1:N 拥有')
    # users → appointments
    rel(2.2, 6.7, 10.8, 8.55, '1:N 预约')
    # properties → appointments
    rel(7.8, 7.9, 9.5, 7.9, '1:N')
    # appointments → payment_orders
    rel(12.1, 7.9, 12.9, 7.9, '1:1')
    # users → messages
    rel(2.2, 6.5, 2.2, 5.7, '1:N 发送')
    # users → reviews
    rel(3.5, 6.8, 4.7, 4.85, '1:N 评价')
    # users → support_tickets
    rel(3.5, 6.8, 8.7, 4.9, '1:N 工单')
    # users → house_requirements
    rel(3.5, 7.6, 12.5, 4.9, '1:N 需求')

    plt.tight_layout(pad=0.3)
    path = os.path.join(OUT, 'fig5-1-er.png')
    plt.savefig(path, dpi=180, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close()
    print(f"  ✔ ER图: {path}")


# ════════════════════════════════════════════════════════════════════════
# 图3：核心类图
# ════════════════════════════════════════════════════════════════════════
def fig_class():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.text(8, 9.6, '系统核心类图', ha='center', fontsize=13, fontweight='bold', color=C_DARK)

    def cls(ax, x, y, name, attrs, methods, w=3.2, fc=C_BLUE):
        row_h = 0.28
        header_h = 0.40
        body = attrs + ['─'*18] + methods
        total_h = header_h + row_h * len(body)
        # header
        hdr = FancyBboxPatch((x-w/2, y), w, header_h,
                             boxstyle="round,pad=0.02", facecolor=fc, edgecolor='#7F8C8D', lw=1, zorder=3)
        ax.add_patch(hdr)
        ax.text(x, y+header_h/2, f'《Entity》\n{name}', ha='center', va='center',
                fontsize=8, color='white', fontweight='bold', zorder=4)
        # body
        body_rect = FancyBboxPatch((x-w/2, y-row_h*len(body)), w, row_h*len(body),
                                   boxstyle="round,pad=0.01", facecolor='#FAFAFA',
                                   edgecolor='#BDC3C7', lw=0.8, zorder=3)
        ax.add_patch(body_rect)
        for i, txt in enumerate(body):
            fy = y - row_h*(i+0.5)
            ax.text(x-w/2+0.1, fy, txt, ha='left', va='center', fontsize=7,
                    color=C_GRAY if txt.startswith('─') else C_DARK, zorder=4)
        return y - row_h*len(body)  # bottom y

    # User
    cls(ax, 2.2, 9.0, 'User', [
        '- id: Long',
        '- username: String',
        '- password: String',
        '- email: String',
        '- role: Role',
        '- enabled: boolean',
    ], [
        '+ getAuthorities()',
    ], fc=C_BLUE)

    # Property
    cls(ax, 6.5, 9.0, 'Property', [
        '- id: Long',
        '- title: String',
        '- price: BigDecimal',
        '- address: String',
        '- area: Double',
        '- layout: String',
        '- status: Status',
        '- owner: User',
    ], [
        '+ getOwner(): User',
    ], fc=C_GREEN)

    # Appointment
    cls(ax, 11.0, 9.0, 'Appointment', [
        '- id: Long',
        '- user: User',
        '- property: Property',
        '- appointmentTime: LDT',
        '- status: Status',
        '- message: String',
    ], [
        '+ getStatus(): Status',
    ], fc=C_ORANGE)

    # PaymentOrder
    cls(ax, 2.2, 4.5, 'PaymentOrder', [
        '- id: Long',
        '- outTradeNo: String',
        '- payer: User',
        '- property: Property',
        '- appointment: Appointment',
        '- amount: BigDecimal',
        '- status: PaymentStatus',
    ], [
        '+ pay(): void',
    ], fc=C_RED)

    # Message
    cls(ax, 6.5, 4.5, 'Message', [
        '- id: Long',
        '- sender: User',
        '- receiver: User',
        '- property: Property',
        '- content: String',
        '- isRead: boolean',
    ], [
        '+ markRead(): void',
    ], fc=C_PURPLE)

    # Review
    cls(ax, 11.0, 4.5, 'Review', [
        '- id: Long',
        '- user: User',
        '- property: Property',
        '- rating: Integer',
        '- content: String',
    ], [
        '+ getRating(): int',
    ], fc='#1ABC9C')

    # 关联线
    def assoc(x1,y1,x2,y2,label='',style='->'):
        ax.annotate('',xy=(x2,y2),xytext=(x1,y1),
                    arrowprops=dict(arrowstyle=style,color='#7F8C8D',lw=1.2),zorder=2)
        if label:
            ax.text((x1+x2)/2,(y1+y2)/2+0.1,label,fontsize=6.5,color='#7F8C8D',ha='center')

    assoc(3.8, 8.55, 5.0, 8.55, '1 拥有 N', '->')
    assoc(8.1, 8.2, 9.4, 8.2, '1 关联 N', '->')
    assoc(2.2, 7.15, 2.2, 5.65, '1:N 支付', '->')
    assoc(6.5, 7.15, 6.5, 5.65, '发送', '->')
    assoc(11.0, 7.15, 11.0, 5.65, '评价', '->')

    plt.tight_layout(pad=0.3)
    path = os.path.join(OUT, 'fig5-2-class.png')
    plt.savefig(path, dpi=180, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close()
    print(f"  ✔ 类图: {path}")


# ════════════════════════════════════════════════════════════════════════
# 图4：预约与支付时序图
# ════════════════════════════════════════════════════════════════════════
def fig_sequence():
    fig, ax = plt.subplots(figsize=(14, 10))
    ax.set_xlim(0, 14); ax.set_ylim(0, 10)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.text(7, 9.65, '预约看房与意向金支付时序图',
            ha='center', fontsize=13, fontweight='bold', color=C_DARK)

    # 参与者
    actors = [('用户\n浏览器', 1.5, C_BLUE),
              ('前端\nVue3', 3.5, C_GREEN),
              ('后端\nSpring Boot', 7.0, C_ORANGE),
              ('数据库\nMySQL', 11.0, C_PURPLE),
              ('支付模块\nPaymentSvc', 12.8, C_RED)]

    top_y = 9.2
    bottom_y = 0.4
    for name, x, c in actors:
        box(ax, x, top_y, 1.6, 0.55, name, fc=c, fs=8)
        ax.plot([x, x], [top_y-0.28, bottom_y], color='#BDC3C7', lw=1.2, ls='--', zorder=1)

    def msg(y, x1, x2, text, ret=False, note=''):
        color = C_GRAY if ret else C_DARK
        ls = '--' if ret else '-'
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
                    arrowprops=dict(arrowstyle='<-' if ret else '->', color=color, lw=1.3))
        mx = (x1+x2)/2
        ax.text(mx, y+0.1, text, ha='center', va='bottom', fontsize=8,
                color=color, style='italic' if ret else 'normal')
        if note:
            ax.text(max(x1,x2)+0.1, y, note, ha='left', va='center', fontsize=7,
                    color=C_GRAY)

    def act(x, y, h, label, fc=C_LIGHT):
        rect = FancyBboxPatch((x-0.18, y-h), 0.36, h,
                              boxstyle="round,pad=0.02", facecolor=fc,
                              edgecolor='#7F8C8D', lw=1, zorder=3)
        ax.add_patch(rect)

    def sep(y, text):
        ax.plot([0.5, 13.5], [y, y], color='#BDC3C7', lw=0.7, ls=':')
        ax.text(0.5, y+0.05, text, fontsize=7.5, color=C_GRAY, va='bottom')

    sep(8.8, '① 用户选择房源，提交预约')
    msg(8.5, 1.5, 3.5, 'POST /appointments\n{propertyId, time}')
    msg(8.2, 3.5, 7.0, '转发请求')
    act(7.0, 8.2, 0.4)
    msg(7.9, 7.0, 11.0, 'SELECT 检查时间冲突')
    msg(7.7, 11.0, 7.0, '无冲突', ret=True)
    msg(7.5, 7.0, 11.0, 'INSERT appointment')
    msg(7.3, 7.0, 3.5, '返回预约ID', ret=True)
    msg(7.1, 3.5, 1.5, '预约成功提示', ret=True)

    sep(6.8, '② 用户创建支付订单')
    msg(6.5, 1.5, 3.5, 'POST /payments/create\n{appointmentId}')
    msg(6.2, 3.5, 7.0, '转发请求')
    act(7.0, 6.2, 0.35)
    msg(5.9, 7.0, 11.0, 'SELECT property.price')
    msg(5.7, 11.0, 7.0, 'price=50000', ret=True)
    msg(5.5, 7.0, 12.8, '生成订单号\n计算意向金')
    msg(5.2, 12.8, 11.0, 'INSERT payment_order')
    msg(5.0, 12.8, 7.0, '返回qrCodeUrl', ret=True)
    msg(4.8, 7.0, 3.5, '返回订单信息', ret=True)
    msg(4.6, 3.5, 1.5, '展示支付二维码', ret=True)

    sep(4.3, '③ 用户确认支付（模拟）')
    msg(4.0, 1.5, 3.5, 'POST /payments/{id}/mock-pay')
    msg(3.8, 3.5, 7.0, '转发')
    act(7.0, 3.8, 0.3)
    msg(3.5, 7.0, 12.8, '校验订单状态')
    msg(3.2, 12.8, 11.0, 'UPDATE payment status=PAID')
    msg(3.0, 12.8, 11.0, 'UPDATE appointment status=CONFIRMED')
    msg(2.8, 12.8, 7.0, '支付成功', ret=True)
    msg(2.6, 7.0, 3.5, '200 OK', ret=True)
    msg(2.4, 3.5, 1.5, '支付完成，预约已确认', ret=True)

    plt.tight_layout(pad=0.3)
    path = os.path.join(OUT, 'fig5-3-sequence.png')
    plt.savefig(path, dpi=180, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close()
    print(f"  ✔ 时序图: {path}")


# ════════════════════════════════════════════════════════════════════════
# 图5：预约活动图
# ════════════════════════════════════════════════════════════════════════
def fig_activity():
    fig, ax = plt.subplots(figsize=(10, 14))
    ax.set_xlim(0, 10); ax.set_ylim(0, 14)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.text(5, 13.6, '预约看房活动图',
            ha='center', fontsize=13, fontweight='bold', color=C_DARK)

    def abox(y, text, fc=C_BLUE, w=4.0):
        box(ax, 5, y, w, 0.55, text, fc=fc, fs=9)

    def diamond(y, text):
        d = 0.4
        xs = [5, 5+d*1.8, 5, 5-d*1.8, 5]
        ys = [y+d, y, y-d, y, y+d]
        ax.fill(xs, ys, color=C_ORANGE, zorder=3, alpha=0.9)
        ax.plot(xs, ys, color='white', lw=1, zorder=4)
        ax.text(5, y, text, ha='center', va='center', fontsize=8,
                color='white', fontweight='bold', zorder=5)

    def down(y1, y2):
        ax.annotate('', xy=(5, y2+0.28), xytext=(5, y1-0.28),
                    arrowprops=dict(arrowstyle='->', color=C_DARK, lw=1.3))

    def branch(y, left_text, right_text, left_y, right_y):
        # left
        ax.annotate('', xy=(3.0, left_y+0.28), xytext=(3.6, y),
                    arrowprops=dict(arrowstyle='->', color=C_DARK, lw=1.1))
        ax.text(3.3, y+0.1, left_text, fontsize=8, ha='center', color=C_DARK)
        # right
        ax.annotate('', xy=(7.0, right_y+0.28), xytext=(6.4, y),
                    arrowprops=dict(arrowstyle='->', color=C_DARK, lw=1.1))
        ax.text(6.8, y+0.1, right_text, fontsize=8, ha='center', color=C_DARK)

    # 开始
    circle = plt.Circle((5, 13.1), 0.2, color=C_DARK, zorder=4)
    ax.add_patch(circle)
    down(13.1, 12.5)

    abox(12.5, '用户登录系统', fc=C_BLUE)
    down(12.5, 11.8)
    abox(11.8, '浏览房源列表', fc=C_BLUE)
    down(11.8, 11.1)
    abox(11.1, '进入房源详情页', fc=C_BLUE)
    down(11.1, 10.4)
    abox(10.4, '选择预约时间', fc=C_GREEN)
    down(10.4, 9.7)
    abox(9.7, '提交预约请求', fc=C_GREEN)
    down(9.7, 9.0)
    diamond(9.0, '时间冲突？')

    # 冲突分支
    ax.annotate('', xy=(3.0, 8.7), xytext=(4.3, 9.0),
                arrowprops=dict(arrowstyle='->', color=C_RED, lw=1.1))
    ax.text(3.3, 9.15, '是', fontsize=8, color=C_RED, ha='center')
    box(ax, 3.0, 8.4, 2.4, 0.5, '提示时间冲突\n请重新选择', fc=C_RED, fs=8)
    ax.annotate('', xy=(3.0, 10.4), xytext=(3.0, 8.65),
                arrowprops=dict(arrowstyle='->', color=C_RED, lw=1.0))

    ax.annotate('', xy=(5, 8.55), xytext=(5.7, 9.0),
                arrowprops=dict(arrowstyle='->', color=C_GREEN, lw=1.1))
    ax.text(5.7, 9.15, '否', fontsize=8, color=C_GREEN, ha='center')

    abox(8.3, '生成预约记录（待处理）', fc=C_GREEN)
    down(8.3, 7.6)
    abox(7.6, '创建支付订单', fc=C_ORANGE)
    down(7.6, 6.9)
    abox(6.9, '用户完成模拟支付', fc=C_ORANGE)
    down(6.9, 6.2)
    diamond(6.2, '支付成功？')

    # 支付失败
    ax.annotate('', xy=(3.0, 5.9), xytext=(4.3, 6.2),
                arrowprops=dict(arrowstyle='->', color=C_RED, lw=1.1))
    ax.text(3.3, 6.35, '失败', fontsize=8, color=C_RED, ha='center')
    box(ax, 3.0, 5.6, 2.4, 0.5, '记录失败原因\n支付状态=FAILED', fc=C_RED, fs=8)

    ax.annotate('', xy=(5, 5.85), xytext=(5.7, 6.2),
                arrowprops=dict(arrowstyle='->', color=C_GREEN, lw=1.1))
    ax.text(5.8, 6.35, '成功', fontsize=8, color=C_GREEN, ha='center')

    abox(5.5, '更新订单状态=PAID', fc=C_GREEN)
    down(5.5, 4.8)
    abox(4.8, '联动预约状态=CONFIRMED', fc=C_GREEN)
    down(4.8, 4.1)
    abox(4.1, '通知用户预约已确认', fc=C_BLUE)
    down(4.1, 3.4)
    abox(3.4, '用户查看预约时间线', fc=C_BLUE)
    down(3.4, 2.7)

    # 结束
    end = plt.Circle((5, 2.4), 0.2, color=C_DARK, zorder=4)
    ax.add_patch(end)
    end2 = plt.Circle((5, 2.4), 0.32, color='none', ec=C_DARK, lw=2.5, zorder=4)
    ax.add_patch(end2)

    plt.tight_layout(pad=0.3)
    path = os.path.join(OUT, 'fig5-4-activity.png')
    plt.savefig(path, dpi=180, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close()
    print(f"  ✔ 活动图: {path}")


# ════════════════════════════════════════════════════════════════════════
# 主入口
# ════════════════════════════════════════════════════════════════════════
if __name__ == '__main__':
    print("生成论文图表...")
    fig_architecture()
    fig_er()
    fig_class()
    fig_sequence()
    fig_activity()
    print(f"\n✔ 全部图表保存至: {OUT}")

