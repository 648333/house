package com.trae.housing;

import com.trae.housing.model.Appointment;
import com.trae.housing.model.HouseRequirement;
import com.trae.housing.model.Property;
import com.trae.housing.model.Review;
import com.trae.housing.model.User;
import com.trae.housing.repository.AppointmentRepository;
import com.trae.housing.repository.HouseRequirementRepository;
import com.trae.housing.repository.MessageRepository;
import com.trae.housing.repository.PaymentOrderRepository;
import com.trae.housing.repository.PriceAlertSubscriptionRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.ReviewRepository;
import com.trae.housing.repository.SupportTicketRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.repository.AgentScheduleSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PriceAlertSubscriptionRepository priceAlertSubscriptionRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private AgentScheduleSlotRepository agentScheduleSlotRepository;

    @Autowired
    private HouseRequirementRepository requirementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        messageRepository.deleteAll();
        priceAlertSubscriptionRepository.deleteAll();
        supportTicketRepository.deleteAll();
        agentScheduleSlotRepository.deleteAll();
        paymentOrderRepository.deleteAll();
        appointmentRepository.deleteAll();
        reviewRepository.deleteAll();
        requirementRepository.deleteAll();
        propertyRepository.deleteAll();

        User user1 = createUserIfNotFound("user1", "user1@example.com", "password", User.Role.USER);
        User admin = createUserIfNotFound("admin", "admin@example.com", "password", User.Role.ADMIN);
        User agent1 = createUserIfNotFound("agent1", "agent1@example.com", "password", User.Role.AGENT);
        User agent2 = createUserIfNotFound("agent2", "agent2@example.com", "password", User.Role.AGENT);

        List<Property> seededProperties = createProperties(agent1, agent2);
        seedUserBehavior(user1, admin, seededProperties);
        seedRequirements(user1, agent1, seededProperties);
    }

    private User createUserIfNotFound(String username, String email, String password, User.Role role) {
        User existingUser = userRepository.findByUsername(username).orElse(null);
        if (existingUser != null) {
            existingUser.setPassword(passwordEncoder.encode(password));
            existingUser.setRole(role);
            existingUser.setEmail(email);
            existingUser.setEnabled(true);
            return userRepository.save(existingUser);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private List<Property> createProperties(User agent1, User agent2) {
        return List.of(
                saveProperty("浦东云幕江景大平层", "浦东新区滨江大道 188 号",
                        "江景大平层，客厅面宽开阔，适合追求高级感和安静生活节奏的用户。",
                        new BigDecimal("980"), 168.0, 31.2356, 121.5074, "公寓", "精装修",
                        "南北通透", "高层/32层", "2019年", "4室2厅", "近地铁,江景,大平层,精装修,高端社区,落地窗",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(2)),
                saveProperty("静安法式奶油风两居", "静安区常德路 520 号",
                        "奶油风两居，配色柔和，通勤便利，适合年轻情侣或独居白领。",
                        new BigDecimal("460"), 78.0, 31.2332, 121.4471, "普通住宅", "精装修",
                        "南向", "中层/18层", "2016年", "2室1厅", "奶油风,近商圈,地铁口,拎包入住,通勤方便",
                        "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(1)),
                saveProperty("徐汇学区安静三居", "徐汇区漕溪北路 288 号",
                        "邻近优质学校，社区安静，三房两卫布局适合家庭长期居住。",
                        new BigDecimal("620"), 106.0, 31.1936, 121.4369, "普通住宅", "简约装修",
                        "南北通透", "中高层/20层", "2014年", "3室2厅", "学区房,家庭友好,安静社区,近公园,三居室",
                        "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(4)),
                saveProperty("虹桥轻奢商务公寓", "长宁区娄山关路 666 号",
                        "商务感精装公寓，近机场与会展中心，适合差旅频繁的用户。",
                        new BigDecimal("520"), 69.0, 31.2094, 121.3980, "公寓", "轻奢装修",
                        "东南", "高层/28层", "2020年", "1室1厅", "商务公寓,近机场,轻奢,地铁口,拎包入住",
                        "https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(3)),
                saveProperty("前滩艺术感 Loft", "浦东新区前滩大道 99 号",
                        "挑高 Loft，适合喜欢设计感和灵活空间的人群，拍照也很出片。",
                        new BigDecimal("540"), 82.0, 31.1662, 121.4607, "Loft", "设计师装修",
                        "西南", "高层/22层", "2021年", "2室2厅", "Loft,艺术感,设计师风格,年轻社区,宠物友好",
                        "https://images.unsplash.com/photo-1449844908441-8829872d2607?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(5)),
                saveProperty("杨浦地铁口一人居", "杨浦区国权路 120 号",
                        "通勤效率高的一居室，步行可到地铁，周边生活气息浓厚。",
                        new BigDecimal("320"), 46.0, 31.3012, 121.5168, "普通住宅", "简装",
                        "南向", "中层/12层", "2012年", "1室1厅", "近地铁,小户型,低总价,通勤友好,一人居",
                        "https://images.unsplash.com/photo-1505692952047-1a78307da8f2?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(6)),
                saveProperty("松江花园叠墅", "松江区广富林路 808 号",
                        "带花园和露台的叠墅，适合多代同堂，也适合喜欢松弛感生活的人。",
                        new BigDecimal("1180"), 210.0, 31.0397, 121.2277, "别墅", "精装修",
                        "南北", "1-3层", "2018年", "5室3厅", "花园,别墅,露台,家庭改善,双车位",
                        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(8)),
                saveProperty("宝山高性价比三房", "宝山区友谊路 368 号",
                        "预算友好的三房，适合首套改善，配套成熟。",
                        new BigDecimal("380"), 92.0, 31.4046, 121.4891, "普通住宅", "简约装修",
                        "南北通透", "中层/16层", "2011年", "3室1厅", "高性价比,首套友好,成熟配套,家庭友好",
                        "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(7)),
                saveProperty("闵行社区亲子四居", "闵行区七莘路 998 号",
                        "社区绿化好，近学校和商场，四居室适合成长型家庭。",
                        new BigDecimal("760"), 138.0, 31.1234, 121.3864, "普通住宅", "精装修",
                        "南北通透", "中高层/24层", "2017年", "4室2厅", "亲子家庭,学区房,近商场,大四居,社区成熟",
                        "https://images.unsplash.com/photo-1560185008-b033106af5c3?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(2)),
                saveProperty("普陀治愈原木风小宅", "普陀区真如路 118 号",
                        "原木风格小宅，氛围柔和，适合喜欢治愈感空间的年轻用户。",
                        new BigDecimal("350"), 52.0, 31.2521, 121.4026, "公寓", "原木风装修",
                        "南向", "中层/14层", "2018年", "1室1厅", "治愈风,原木风,小户型,近地铁,独居友好",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusHours(18)),
                saveProperty("黄浦老洋房精装两居", "黄浦区复兴中路 86 号",
                        "老洋房气质十足，内装更新完整，适合偏爱城市质感的人。",
                        new BigDecimal("680"), 88.0, 31.2148, 121.4741, "洋房", "精装修",
                        "南向", "低层/6层", "2008年翻新", "2室2厅", "老洋房,市中心,精装,历史街区,氛围感",
                        "https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(9)),
                saveProperty("青浦湖景度假别墅", "青浦区淀山湖大道 588 号",
                        "湖景别墅，适合周末度假和长住结合，私密性很好。",
                        new BigDecimal("1360"), 245.0, 31.1034, 120.9861, "别墅", "豪华装修",
                        "南北", "独栋", "2020年", "5室4厅", "湖景,别墅,度假感,私密性强,大宅",
                        "https://images.unsplash.com/photo-1513584684374-8bab748fbf90?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(12)),
                saveProperty("嘉定新盘样板两居", "嘉定区阿克苏路 188 号",
                        "新盘样板间风格，采光好，适合首置或婚房需求。",
                        new BigDecimal("410"), 74.0, 31.3751, 121.2651, "新房", "精装修",
                        "南向", "高层/26层", "2024年", "2室2厅", "新房,婚房,品牌开发商,近地铁,高性价比",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusHours(10)),
                saveProperty("临港海风公寓", "浦东新区临港大道 600 号",
                        "靠海公寓，适合喜欢开阔视野和安静生活节奏的人。",
                        new BigDecimal("430"), 72.0, 30.9102, 121.8898, "公寓", "现代装修",
                        "东南", "高层/20层", "2022年", "2室1厅", "海景,安静社区,现代风,新片区,治愈感",
                        "https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(3)),
                saveProperty("长宁宠物友好温馨两居", "长宁区仙霞路 230 号",
                        "宠物友好社区，两居室动线舒适，生活氛围轻松。",
                        new BigDecimal("470"), 80.0, 31.2180, 121.3822, "普通住宅", "温馨装修",
                        "南向", "中层/15层", "2015年", "2室1厅", "宠物友好,治愈风,温馨装修,近公园,两居室",
                        "https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(5)),
                saveProperty("浦东金融城高层公寓", "浦东新区银城中路 188 号",
                        "视野开阔，商务人士友好，适合高频通勤和高品质居住。",
                        new BigDecimal("860"), 128.0, 31.2401, 121.5012, "公寓", "轻奢装修",
                        "东南", "高层/38层", "2021年", "3室2厅", "金融城,高层景观,商务人士,近地铁,高端社区",
                        "https://images.unsplash.com/photo-1519643381401-22c77e60520e?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusHours(8)),
                saveProperty("徐汇河畔改善四居", "徐汇区龙腾大道 2600 号",
                        "河景改善四居，兼顾景观与功能，适合高品质家庭生活。",
                        new BigDecimal("1280"), 186.0, 31.1791, 121.4548, "大平层", "豪华装修",
                        "南北通透", "高层/30层", "2020年", "4室2厅", "河景,改善房,高端社区,四居室,落地窗",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.PENDING, agent1, LocalDateTime.now().minusHours(5)),
                saveProperty("浦江镇花园联排", "闵行区浦江镇江月路 818 号",
                        "联排带院子，适合大家庭和喜欢院落生活的人群。",
                        new BigDecimal("980"), 198.0, 31.0836, 121.5099, "别墅", "精装修",
                        "南北", "1-3层", "2016年", "5室2厅", "联排,花园,家庭改善,院子,双卫",
                        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.SOLD, agent2, LocalDateTime.now().minusDays(14)),
                saveProperty("Lujiazui Sky Duplex", "Pudong Century Avenue 1000",
                        "A duplex with skyline exposure, wide living room frontage, and a stronger business-living balance for buyers who value both views and commute speed.",
                        new BigDecimal("1320"), 188.0, 31.2347, 121.5175, "Duplex", "Luxury Finish",
                        "South East", "High/40", "2022", "4BR 2LR", "skyline,duplex,subway,business district,river view",
                        "https://images.unsplash.com/photo-1519643381401-22c77e60520e?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusHours(6)),
                saveProperty("Minhang Garden Family Home", "Minhang Hongxin Road 1088",
                        "A family-focused community home with greener surroundings, larger children's room layouts, and practical daily living convenience.",
                        new BigDecimal("690"), 132.0, 31.1622, 121.3749, "Residence", "Fine",
                        "South North", "Mid/18", "2018", "4BR 2LR", "family,school,park,community,improved housing",
                        "https://images.unsplash.com/photo-1568605114967-8130f3a36994?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(2)),
                saveProperty("Yangpu Creative Loft", "Yangpu Anbo Road 66",
                        "A loft with flexible zoning, open kitchen layout, and a younger neighborhood vibe close to universities and maker offices.",
                        new BigDecimal("510"), 84.0, 31.2942, 121.5124, "Loft", "Designer",
                        "East South", "High/20", "2021", "2BR 1LR", "loft,creative,young community,subway,pet friendly",
                        "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(4)),
                saveProperty("Qibao Metro Starter Home", "Minhang Qibao Zhongyi Road 218",
                        "Compact but efficient for first-home buyers, with a straightforward commute and strong day-to-day retail support nearby.",
                        new BigDecimal("355"), 58.0, 31.1577, 121.3495, "Apartment", "Simple",
                        "South", "Mid/11", "2015", "1BR 1LR", "starter home,subway,compact,budget friendly,commute",
                        "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(1)),
                saveProperty("Hongqiao Business Residence", "Changning Tianshan Road 999",
                        "A polished business residence suited for consultants and finance professionals who want airport and rail access without giving up comfort.",
                        new BigDecimal("790"), 118.0, 31.2193, 121.3925, "Residence", "Light Luxury",
                        "South", "High/26", "2019", "3BR 2LR", "business,airport access,subway,high rise,light luxury",
                        "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusHours(14)),
                saveProperty("Songjiang Lakeside Villa", "Songjiang Chenhua Highway 77",
                        "A lower-density villa with stronger holiday living feel, private outdoor space, and more relaxed weekend usage.",
                        new BigDecimal("1490"), 268.0, 31.0501, 121.2145, "Villa", "Luxury Finish",
                        "South North", "Detached", "2021", "5BR 2LR", "villa,lakeside,garden,holiday,privacy",
                        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusDays(11)),
                saveProperty("Jingan Boutique Two Bedroom", "Jingan Wuding Road 399",
                        "A boutique downtown two-bedroom home with a calmer interior palette and stronger walkability for coffee shops and nightlife.",
                        new BigDecimal("598"), 79.0, 31.2298, 121.4528, "Apartment", "Fine",
                        "South", "Mid/9", "2017", "2BR 1LR", "downtown,walkable,boutique,two bedroom,cafe",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(3)),
                saveProperty("Baoshan Riverfront Three Bedroom", "Baoshan Mudanjiang Road 520",
                        "A higher-value three-bedroom option with clearer separation between public and private zones and good room sizes for a growing family.",
                        new BigDecimal("428"), 101.0, 31.4013, 121.4862, "Residence", "Fine",
                        "South North", "Mid/17", "2016", "3BR 2LR", "value,family,riverfront,three bedroom,community",
                        "https://images.unsplash.com/photo-1560185008-b033106af5c3?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent2, LocalDateTime.now().minusHours(20)),
                saveProperty("Qingpu Low Density Courtyard Home", "Qingpu Zhujiajiao Zhuhu Road 90",
                        "A quieter low-density home with private courtyard use and a pace better suited to buyers who favor weekend-like everyday living.",
                        new BigDecimal("880"), 156.0, 31.1112, 120.9231, "Townhouse", "Fine",
                        "South", "1-2", "2019", "4BR 2LR", "courtyard,low density,quiet,family,improved housing",
                        "https://images.unsplash.com/photo-1513584684374-8bab748fbf90?auto=format&fit=crop&w=1400&q=80",
                        Property.Status.APPROVED, agent1, LocalDateTime.now().minusDays(6))
        );
    }

    private Property saveProperty(String title, String address, String description, BigDecimal price,
                                  Double area, Double latitude, Double longitude, String type,
                                  String decoration, String orientation, String floor, String yearBuilt,
                                  String layout, String tags, String imageUrl, Property.Status status,
                                  User owner, LocalDateTime createdAt) {
        Property property = new Property();
        property.setTitle(title);
        property.setAddress(address);
        property.setDescription(description);
        property.setPrice(price);
        property.setArea(area);
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        property.setType(type);
        property.setDecoration(decoration);
        property.setOrientation(orientation);
        property.setFloor(floor);
        property.setYearBuilt(yearBuilt);
        property.setLayout(layout);
        property.setTags(tags);
        property.setImageUrl(imageUrl);
        property.setFloorPlanUrl(resolveFloorPlanUrl(title, layout));
        property.setModel3dUrl(resolveModelUrl(title, layout));
        property.setPanoramaUrl(imageUrl);
        property.setStatus(status);
        property.setOwner(owner);
        property.setContactName(owner.getUsername());
        property.setCreatedAt(createdAt);
        return propertyRepository.save(property);
    }

    private String resolveFloorPlanUrl(String title, String layout) {
        String titleText = String.valueOf(title).toLowerCase();
        String layoutText = String.valueOf(layout).toLowerCase();

        if (titleText.contains("loft") || layoutText.contains("loft")) {
            return "/floorplans/loft-c.svg";
        }
        if (layoutText.contains("1") || layoutText.contains("2") || layoutText.contains("一") || layoutText.contains("二")) {
            return "/floorplans/compact-b.svg";
        }
        return "/floorplans/family-a.svg";
    }

    private String resolveModelUrl(String title, String layout) {
        String titleText = String.valueOf(title).toLowerCase();
        String layoutText = String.valueOf(layout).toLowerCase();

        if (titleText.contains("loft") || layoutText.contains("loft")) {
            return "/models/apartment-2.glb";
        }
        if (layoutText.contains("1") || layoutText.contains("2") || layoutText.contains("一") || layoutText.contains("二")) {
            return "/models/apartment-2.glb";
        }
        return "/models/house-interiors.glb";
    }

    private void seedUserBehavior(User user1, User admin, List<Property> properties) {
        createAppointment(user1, properties.get(1), LocalDateTime.now().plusDays(1), "我想看一下这套温馨两居。");
        createAppointment(user1, properties.get(2), LocalDateTime.now().plusDays(2), "更关注学区和社区安静程度。");
        createAppointment(user1, properties.get(9), LocalDateTime.now().plusDays(3), "想找原木风和治愈感更强的房源。");

        createReview(user1, properties.get(1), 5, "很喜欢这个房子的光线和色调，适合长期住。");
        createReview(user1, properties.get(9), 4, "空间不大但氛围很好，很适合独居。");
        createReview(admin, properties.get(0), 5, "房源品质不错，信息也很完整。");
    }

    private void createAppointment(User user, Property property, LocalDateTime appointmentTime, String message) {
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setProperty(property);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(Appointment.Status.PENDING);
        appointment.setMessage(message);
        appointmentRepository.save(appointment);
    }

    private void createReview(User user, Property property, int rating, String content) {
        Review review = new Review();
        review.setUser(user);
        review.setProperty(property);
        review.setRating(rating);
        review.setContent(content);
        reviewRepository.save(review);
    }

    private void seedRequirements(User user1, User agent1, List<Property> properties) {
        HouseRequirement requirement = new HouseRequirement();
        requirement.setTitle("徐汇或静安通勤改善型两居");
        requirement.setPreferredArea("徐汇,静安");
        requirement.setPropertyType("普通住宅");
        requirement.setLayoutPreference("2室");
        requirement.setMinArea(70.0);
        requirement.setMaxArea(110.0);
        requirement.setMinBudget(new BigDecimal("420"));
        requirement.setMaxBudget(new BigDecimal("650"));
        requirement.setCommutePreference("近地铁");
        requirement.setLifestyleTags("治愈风,近地铁,通勤方便");
        requirement.setNote("希望社区安静，适合两人长期居住。");
        requirement.setUser(user1);
        requirement.setAssignedAgent(agent1);
        requirement.setStatus(HouseRequirement.Status.FOLLOWING);
        requirementRepository.save(requirement);
    }
}
