$ErrorActionPreference = 'Stop'

function RGBInt([int]$r, [int]$g, [int]$b) {
    return $r + ($g * 256) + ($b * 65536)
}

function Add-FullRect($slide, [double]$left, [double]$top, [double]$width, [double]$height, [int]$fillColor, [int]$lineColor = -1, [double]$lineWeight = 0.0) {
    $shape = $slide.Shapes.AddShape(1, $left, $top, $width, $height)
    $shape.Fill.Visible = -1
    $shape.Fill.ForeColor.RGB = $fillColor
    if ($lineColor -eq -1) {
        $shape.Line.Visible = 0
    } else {
        $shape.Line.Visible = -1
        $shape.Line.ForeColor.RGB = $lineColor
        $shape.Line.Weight = $lineWeight
    }
    return $shape
}

function Add-RoundedRect($slide, [double]$left, [double]$top, [double]$width, [double]$height, [int]$fillColor, [int]$lineColor, [double]$lineWeight = 1.25) {
    $shape = $slide.Shapes.AddShape(5, $left, $top, $width, $height)
    $shape.Fill.ForeColor.RGB = $fillColor
    $shape.Line.ForeColor.RGB = $lineColor
    $shape.Line.Weight = $lineWeight
    return $shape
}

function Add-Line($slide, [double]$x1, [double]$y1, [double]$x2, [double]$y2, [int]$color, [double]$weight = 1.75, [int]$arrowEnd = 3) {
    $line = $slide.Shapes.AddLine($x1, $y1, $x2, $y2)
    $line.Line.ForeColor.RGB = $color
    $line.Line.Weight = $weight
    if ($arrowEnd -gt 0) {
        $line.Line.EndArrowheadStyle = $arrowEnd
    }
    return $line
}

function Add-Text($slide, [double]$left, [double]$top, [double]$width, [double]$height, [string]$text, [string]$fontName, [double]$fontSize, [int]$fontColor, [bool]$bold = $false, [int]$align = 1) {
    $tb = $slide.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $tb.TextFrame.TextRange.Text = $text
    $tb.TextFrame.TextRange.Font.Name = $fontName
    $tb.TextFrame.TextRange.Font.Size = $fontSize
    $tb.TextFrame.TextRange.Font.Color.RGB = $fontColor
    $tb.TextFrame.TextRange.Font.Bold = $(if ($bold) { -1 } else { 0 })
    $tb.TextFrame.TextRange.ParagraphFormat.Alignment = $align
    $tb.TextFrame.WordWrap = -1
    $tb.Line.Visible = 0
    $tb.Fill.Visible = 0
    return $tb
}

function Add-BulletList($slide, [double]$left, [double]$top, [double]$width, [double]$height, [string[]]$items, [string]$fontName, [double]$fontSize, [int]$fontColor, [double]$spaceAfter = 10) {
    $tb = $slide.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $tb.Line.Visible = 0
    $tb.Fill.Visible = 0
    $tb.TextFrame.WordWrap = -1
    $tb.TextFrame.TextRange.Text = (($items | ForEach-Object { "• $_" }) -join "`r")
    $tb.TextFrame.TextRange.Font.Name = $fontName
    $tb.TextFrame.TextRange.Font.Size = $fontSize
    $tb.TextFrame.TextRange.Font.Color.RGB = $fontColor
    return $tb
}

function Add-SectionHeader($slide, [double]$slideWidth, [string]$title, [string]$tag, [int]$accentColor, [int]$titleColor, [string]$fontName) {
    Add-FullRect $slide 0 0 $slideWidth 540 (RGBInt 246 249 252) | Out-Null
    Add-FullRect $slide 0 0 $slideWidth 18 $accentColor | Out-Null
    Add-Text $slide 48 34 220 24 $tag $fontName 11 (RGBInt 92 119 145) $true 1 | Out-Null
    Add-Text $slide 48 58 420 38 $title $fontName 24 $titleColor $true 1 | Out-Null
}

function Add-Chip($slide, [double]$left, [double]$top, [double]$width, [double]$height, [string]$text, [int]$fillColor, [int]$lineColor, [int]$fontColor, [string]$fontName) {
    $shape = Add-RoundedRect $slide $left $top $width $height $fillColor $lineColor 1
    $shape.Adjustments.Item(1) = 0.2
    $shape.TextFrame.TextRange.Text = $text
    $shape.TextFrame.TextRange.Font.Name = $fontName
    $shape.TextFrame.TextRange.Font.Size = 13
    $shape.TextFrame.TextRange.Font.Color.RGB = $fontColor
    $shape.TextFrame.TextRange.Font.Bold = -1
    $shape.TextFrame.TextRange.ParagraphFormat.Alignment = 2
    $shape.TextFrame.VerticalAnchor = 3
    return $shape
}

$root = 'E:\AI\house\project\daojishi'
$outputPath = Join-Path $root 'docs\中期检查汇报PPT_刘思博_美化版.pptx'
$previewDir = Join-Path $root 'docs\ppt_export_after'

$archImg = Join-Path $root '毕业设计论文材料\figures\fig4-1-system-architecture.png'
$erImg = Join-Path $root '毕业设计论文材料\figures\fig5-1-er.png'
$classImg = Join-Path $root '毕业设计论文材料\figures\fig5-2-class.png'
$activityImg = Join-Path $root '毕业设计论文材料\figures\fig5-4-activity.png'

$font = 'Microsoft YaHei'
$bg = RGBInt 246 249 252
$navy = RGBInt 28 58 96
$blue = RGBInt 66 118 198
$teal = RGBInt 52 166 164
$gold = RGBInt 225 170 90
$muted = RGBInt 92 110 130
$dark = RGBInt 40 56 76
$lineSoft = RGBInt 207 221 235
$cardBlue = RGBInt 233 241 251
$cardTeal = RGBInt 232 247 245
$cardGold = RGBInt 249 241 229
$cardWhite = RGBInt 255 255 255

if (Test-Path $outputPath) {
    Remove-Item $outputPath -Force
}
if (Test-Path $previewDir) {
    Remove-Item $previewDir -Recurse -Force
}
New-Item -ItemType Directory -Path $previewDir | Out-Null

$powerpoint = $null
$presentation = $null

try {
    $powerpoint = New-Object -ComObject PowerPoint.Application
    $powerpoint.Visible = -1
    $presentation = $powerpoint.Presentations.Add()
    $presentation.PageSetup.SlideWidth = 960
    $presentation.PageSetup.SlideHeight = 540

    $slideWidth = $presentation.PageSetup.SlideWidth
    $slideHeight = $presentation.PageSetup.SlideHeight

    $slides = $presentation.Slides

    # Slide 1
    $slide = $slides.Add(1, 12)
    Add-FullRect $slide 0 0 $slideWidth $slideHeight $bg | Out-Null
    Add-FullRect $slide 0 0 14 $slideHeight $blue | Out-Null
    Add-FullRect $slide 14 0 12 $slideHeight $teal | Out-Null
    Add-RoundedRect $slide 622 62 274 372 $cardWhite $lineSoft 1.5 | Out-Null
    Add-FullRect $slide 650 84 218 12 $blue | Out-Null
    Add-FullRect $slide 650 106 158 12 $teal | Out-Null
    Add-FullRect $slide 650 128 190 12 $gold | Out-Null
    Add-RoundedRect $slide 650 168 95 78 $cardBlue $blue 1.2 | Out-Null
    Add-RoundedRect $slide 760 168 108 78 $cardTeal $teal 1.2 | Out-Null
    Add-RoundedRect $slide 650 260 218 130 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 678 188 40 22 'UI' $font 16 $navy $true 2 | Out-Null
    Add-Text $slide 780 188 70 22 'API' $font 16 $navy $true 2 | Out-Null
    Add-Text $slide 677 294 165 24 '房源浏览 / 预约 / 咨询' $font 17 $dark $true 1 | Out-Null
    Add-Text $slide 677 324 165 60 '前后端分离、多角色协同、可视化展示' $font 12 $muted $false 1 | Out-Null
    Add-Text $slide 60 92 480 92 '基于 Spring Boot 的房屋交易平台设计与实现' $font 28 $navy $true 1 | Out-Null
    Add-Text $slide 62 206 380 26 '毕业设计中期检查汇报' $font 16 $teal $true 1 | Out-Null
    Add-Text $slide 62 258 360 26 '刘思博' $font 18 $dark $true 1 | Out-Null
    Add-Text $slide 62 292 420 24 '数据科学与大数据技术' $font 15 $muted $false 1 | Out-Null
    Add-Text $slide 62 322 420 24 '指导教师：阳锋' $font 15 $muted $false 1 | Out-Null
    Add-FullRect $slide 62 366 120 4 $blue | Out-Null
    Add-Text $slide 62 392 420 36 '围绕用户端、经纪人端、管理员端构建完整房屋交易业务链路' $font 14 $dark $false 1 | Out-Null
    Add-Text $slide 650 410 160 18 'Platform Overview' $font 11 $muted $false 2 | Out-Null

    # Slide 2
    $slide = $slides.Add(2, 12)
    Add-SectionHeader $slide $slideWidth '课题背景与研究目标' 'PROJECT CONTEXT' $blue $navy $font
    Add-RoundedRect $slide 48 118 395 320 $cardWhite $lineSoft 1.2 | Out-Null
    Add-RoundedRect $slide 470 118 442 320 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 72 138 120 24 '现实痛点' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 72 178 330 210 @(
        '房源信息分散，用户跨平台比对成本高',
        '预约、咨询、售后流程缺少统一协同入口',
        '传统系统展示能力弱，难体现地图与 3D 看房优势'
    ) $font 16 $dark 14 | Out-Null
    Add-Text $slide 494 138 120 24 '研究目标' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 494 178 370 210 @(
        '构建支持普通用户、经纪人、管理员的多角色平台',
        '打通房源发布、检索、预约、咨询、工单与管理流程',
        '在基础交易之外增强地图找房、推荐、3D 看房等展示效果'
    ) $font 16 $dark 14 | Out-Null
    Add-Text $slide 84 390 300 18 '问题导向' $font 11 $muted $false 1 | Out-Null
    Add-Text $slide 506 390 300 18 '目标导向' $font 11 $muted $false 1 | Out-Null
    Add-Chip $slide 278 456 110 30 '信息整合' $cardBlue $blue $navy $font | Out-Null
    Add-Chip $slide 410 456 110 30 '流程协同' $cardTeal $teal $navy $font | Out-Null
    Add-Chip $slide 542 456 110 30 '体验提升' $cardGold $gold $navy $font | Out-Null

    # Slide 3
    $slide = $slides.Add(3, 12)
    Add-SectionHeader $slide $slideWidth '技术路线与系统架构' 'ARCHITECTURE' $blue $navy $font
    Add-RoundedRect $slide 48 120 250 340 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 70 140 170 24 '技术栈' $font 18 $navy $true 1 | Out-Null
    Add-Chip $slide 70 188 180 34 'Vue 3 + Vite + Element Plus' $cardBlue $blue $navy $font | Out-Null
    Add-Chip $slide 70 234 180 34 'Spring Boot + Security + JWT' $cardTeal $teal $navy $font | Out-Null
    Add-Chip $slide 70 280 115 34 'JPA / Hibernate' $cardGold $gold $navy $font | Out-Null
    Add-Chip $slide 70 326 86 34 'MySQL' $cardBlue $blue $navy $font | Out-Null
    Add-Chip $slide 70 372 150 34 'ECharts / Leaflet / 3D' $cardTeal $teal $navy $font | Out-Null
    Add-Text $slide 70 424 190 42 '前后端分离架构，便于功能扩展与后续部署。' $font 13 $muted $false 1 | Out-Null
    Add-RoundedRect $slide 328 120 584 340 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 350 140 160 24 '系统架构图' $font 18 $navy $true 1 | Out-Null
    $pic = $slide.Shapes.AddPicture($archImg, 0, -1, 358, 182, 520, 228)
    $pic.LockAspectRatio = -1
    Add-Text $slide 356 418 500 20 '客户端 -> Vue3 前端 -> Spring Boot API -> MySQL，安全认证与业务服务从后端统一支撑。' $font 12 $muted $false 1 | Out-Null

    # Slide 4
    $slide = $slides.Add(4, 12)
    Add-SectionHeader $slide $slideWidth '数据库设计（ER 图）' 'DATA MODEL' $teal $navy $font
    Add-RoundedRect $slide 48 120 270 340 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 70 140 150 24 '核心实体' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 70 180 220 220 @(
        '用户 `users`',
        '房源 `properties`',
        '预约 `appointments`',
        '支付订单 `payment_orders`',
        '消息 `messages` / 工单 `support_tickets`'
    ) $font 15 $dark 12 | Out-Null
    Add-Text $slide 70 408 210 38 '该设计支持“浏览 - 咨询 - 预约 - 支付 - 售后”完整闭环。' $font 12 $muted $false 1 | Out-Null
    Add-RoundedRect $slide 338 120 574 340 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 360 140 160 24 'ER 关系图' $font 18 $navy $true 1 | Out-Null
    $pic = $slide.Shapes.AddPicture($erImg, 0, -1, 370, 180, 510, 230)
    $pic.LockAspectRatio = -1
    Add-Text $slide 360 418 500 20 '用户与房源、预约、消息、评价等实体形成多维关系，为推荐、统计和流程管理提供数据基础。' $font 12 $muted $false 1 | Out-Null

    # Slide 5
    $slide = $slides.Add(5, 12)
    Add-SectionHeader $slide $slideWidth '系统功能模块' 'FEATURE MODULES' $blue $navy $font
    $cards = @(
        @{X=48;Y=138;W=270;H=110;Title='用户端';Body='注册登录、个人中心、收藏对比、找房需求、预约进度';Fill=$cardBlue;Line=$blue},
        @{X=338;Y=138;W=270;H=110;Title='经纪人端';Body='房源发布、编辑、预约处理、排班管理、工单处理';Fill=$cardTeal;Line=$teal},
        @{X=628;Y=138;W=284;H=110;Title='管理员后台';Body='统计总览、房源审核、用户治理、评论管理、消息中心';Fill=$cardGold;Line=$gold},
        @{X=48;Y=270;W=410;H=128;Title='交易与服务链路';Body='房源检索、详情浏览、在线咨询、预约看房、意向金支付、售后工单';Fill=$cardWhite;Line=$lineSoft},
        @{X=480;Y=270;W=432;H=128;Title='扩展亮点';Body='地图找房、个性化推荐、3D 看房、软装工作室、统计分析与模型训练支撑';Fill=$cardWhite;Line=$lineSoft}
    )
    foreach ($card in $cards) {
        $shape = Add-RoundedRect $slide $card.X $card.Y $card.W $card.H $card.Fill $card.Line 1.4
        $shape.Adjustments.Item(1) = 0.18
        Add-Text $slide ($card.X + 18) ($card.Y + 14) ($card.W - 30) 24 $card.Title $font 18 $navy $true 1 | Out-Null
        Add-Text $slide ($card.X + 18) ($card.Y + 48) ($card.W - 30) ($card.H - 54) $card.Body $font 13 $dark $false 1 | Out-Null
    }
    Add-Line $slide 458 334 480 334 $muted 1.5 0 | Out-Null
    Add-Text $slide 760 420 120 18 '多角色协同 + 可视化展示' $font 11 $muted $false 2 | Out-Null

    # Slide 6
    $slide = $slides.Add(6, 12)
    Add-SectionHeader $slide $slideWidth '当前已完成的主要工作' 'PROJECT STATUS' $teal $navy $font
    Add-RoundedRect $slide 48 126 864 84 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 72 146 150 24 '整体进度' $font 18 $navy $true 1 | Out-Null
    Add-FullRect $slide 220 160 560 18 (RGBInt 227 235 244) | Out-Null
    Add-FullRect $slide 220 160 392 18 $teal | Out-Null
    Add-Text $slide 800 148 70 26 '70%' $font 24 $navy $true 2 | Out-Null
    $statusCards = @(
        @{X=48;Y=236;Title='需求与设计';Body='需求分析、概要设计、数据库设计已完成';Fill=$cardBlue;Line=$blue},
        @{X=268;Y=236;Title='系统基础';Body='前后端框架、数据库初始化、登录鉴权已完成';Fill=$cardTeal;Line=$teal},
        @{X=488;Y=236;Title='核心业务';Body='房源、预约、咨询、评论、统计等流程已跑通';Fill=$cardGold;Line=$gold},
        @{X=708;Y=236;Title='扩展能力';Body='推荐、地图找房、3D 看房、软装工作室已接入';Fill=$cardWhite;Line=$lineSoft}
    )
    foreach ($card in $statusCards) {
        Add-RoundedRect $slide $card.X $card.Y 204 160 $card.Fill $card.Line 1.3 | Out-Null
        Add-Text $slide ($card.X + 16) ($card.Y + 18) 160 24 $card.Title $font 18 $navy $true 1 | Out-Null
        Add-Text $slide ($card.X + 16) ($card.Y + 54) 170 82 $card.Body $font 13 $dark $false 1 | Out-Null
    }
    Add-Text $slide 48 424 410 20 '阶段判断：平台已具备可登录、可演示、可扩展的中期成果形态。' $font 13 $muted $false 1 | Out-Null

    # Slide 7
    $slide = $slides.Add(7, 12)
    Add-SectionHeader $slide $slideWidth '重点功能展示' 'SHOWCASE' $blue $navy $font
    Add-RoundedRect $slide 48 124 422 324 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 70 144 180 24 '关键演示链路' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 70 184 360 170 @(
        '房源列表与多条件筛选',
        '房源详情中的收藏 / 对比 / 预约 / 咨询',
        '管理员后台统计与房源审核',
        '地图找房与 3D 看房展示'
    ) $font 16 $dark 12 | Out-Null
    Add-Text $slide 70 360 330 32 '答辩建议按“用户端 -> 后台端”顺序演示，逻辑更顺。' $font 13 $muted $false 1 | Out-Null
    Add-RoundedRect $slide 500 124 412 152 $cardBlue $blue 1.3 | Out-Null
    Add-Text $slide 524 142 120 22 '类图补充' $font 17 $navy $true 1 | Out-Null
    $pic = $slide.Shapes.AddPicture($classImg, 0, -1, 650, 144, 226, 118)
    $pic.LockAspectRatio = -1
    Add-RoundedRect $slide 500 296 412 152 $cardTeal $teal 1.3 | Out-Null
    Add-Text $slide 524 314 140 22 '预约活动流程' $font 17 $navy $true 1 | Out-Null
    $pic = $slide.Shapes.AddPicture($activityImg, 0, -1, 644, 316, 234, 118)
    $pic.LockAspectRatio = -1

    # Slide 8
    $slide = $slides.Add(8, 12)
    Add-SectionHeader $slide $slideWidth '目前存在的问题与下一步计划' 'RISKS & PLAN' $teal $navy $font
    Add-RoundedRect $slide 48 126 408 328 $cardWhite $lineSoft 1.2 | Out-Null
    Add-RoundedRect $slide 486 126 426 328 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 70 146 150 24 '当前问题' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 70 186 340 210 @(
        '支付链路和部分推荐逻辑仍偏演示性质',
        '前后端联调、异常提示和边界处理还需优化',
        '测试覆盖、论文图表和运行截图仍需继续整理'
    ) $font 15 $dark 12 | Out-Null
    Add-Text $slide 508 146 150 24 '下一步计划' $font 18 $navy $true 1 | Out-Null
    Add-BulletList $slide 508 186 360 210 @(
        '完善系统测试与异常处理，提升稳定性',
        '优化推荐、支付展示与页面细节',
        '补齐论文图表、系统截图和答辩材料'
    ) $font 15 $dark 12 | Out-Null
    Add-Chip $slide 508 388 92 30 '测试强化' $cardBlue $blue $navy $font | Out-Null
    Add-Chip $slide 616 388 92 30 '细节优化' $cardTeal $teal $navy $font | Out-Null
    Add-Chip $slide 724 388 116 30 '论文整理' $cardGold $gold $navy $font | Out-Null

    # Slide 9
    $slide = $slides.Add(9, 12)
    Add-FullRect $slide 0 0 $slideWidth $slideHeight $bg | Out-Null
    Add-FullRect $slide 0 0 $slideWidth 18 $blue | Out-Null
    Add-RoundedRect $slide 80 98 800 300 $cardWhite $lineSoft 1.2 | Out-Null
    Add-Text $slide 0 148 $slideWidth 46 '汇报完毕，感谢各位老师指导' $font 28 $navy $true 2 | Out-Null
    Add-Text $slide 0 208 $slideWidth 24 '答辩时可重点演示：房源检索 -> 详情预约 -> 后台审核 -> 统计展示' $font 16 $muted $false 2 | Out-Null
    Add-Chip $slide 278 286 110 34 '用户端' $cardBlue $blue $navy $font | Out-Null
    Add-Chip $slide 425 286 110 34 '经纪人端' $cardTeal $teal $navy $font | Out-Null
    Add-Chip $slide 572 286 110 34 '管理员端' $cardGold $gold $navy $font | Out-Null
    Add-Text $slide 0 448 $slideWidth 22 'Q & A' $font 18 $navy $true 2 | Out-Null

    $presentation.SaveAs($outputPath)
    $presentation.Export($previewDir, 'PNG')
}
finally {
    if ($presentation -ne $null) {
        $presentation.Close()
    }
    if ($powerpoint -ne $null) {
        $powerpoint.Quit()
    }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}

Write-Output "Saved: $outputPath"
Write-Output "Preview: $previewDir"
