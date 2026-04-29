import warnings; warnings.filterwarnings('ignore')
import os, matplotlib; matplotlib.use('Agg')
import matplotlib.pyplot as plt; import matplotlib.patches as mp
matplotlib.rcParams['font.family'] = ['Courier New','DejaVu Sans Mono','Consolas']
OUT = r'E:\AI\house\project\daojishi\毕业设计论文材料\figures_generated'

auth_code = """@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                req.getUsername(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = jwtUtils.generateJwtToken(auth);
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(new JwtResponse(jwt,
            user.getId(), user.getUsername(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest req) {
        if (userRepo.existsByUsername(req.getUsername()))
            return ResponseEntity.badRequest()
                .body("Error: Username already taken");
        User user = new User(req.getUsername(),
            encoder.encode(req.getPassword()), req.getEmail());
        user.setRole(Role.USER);
        userRepo.save(user);
        return ResponseEntity.ok("User registered successfully");
    }
}"""

fig,ax=plt.subplots(figsize=(12,6)); ax.set_xlim(0,12); ax.set_ylim(0,6); ax.axis('off')
fig.patch.set_facecolor('#1E1E2E'); ax.set_facecolor('#1E1E2E')
tb=mp.FancyBboxPatch((0,5.6),12,0.4,boxstyle='round,pad=0',facecolor='#313244',ec='none',zorder=2)
ax.add_patch(tb)
ax.text(0.3,5.8,'AuthController.java',fontsize=9,color='#CDD6F4',va='center',family='Courier New')
for i,ln in enumerate(auth_code.strip().split('\n')):
    y=5.4-(i*0.27)
    if y<0.1: break
    ax.text(0.2,y,ln,fontsize=7.5,color='#CDD6F4',va='center',family='Courier New',zorder=3)
plt.tight_layout(pad=0)
plt.savefig(os.path.join(OUT,'code-auth.png'),dpi=180,bbox_inches='tight',facecolor='#1E1E2E'); plt.close()
print('code-auth.png done')

pay_code = """@RestController
@RequestMapping("/payments")
public class PaymentController {

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest req,
                                          @AuthenticationPrincipal UserDetails ud) {
        User payer = userRepo.findByUsername(ud.getUsername()).orElseThrow();
        Property prop = propertyRepo.findById(req.getPropertyId()).orElseThrow();
        Appointment appt = appointmentRepo.findById(req.getAppointmentId()).get();
        BigDecimal amount = prop.getPrice().multiply(new BigDecimal("0.02"));
        PaymentOrder order = new PaymentOrder();
        order.setOutTradeNo(UUID.randomUUID().toString().replace("-",""));
        order.setPayer(payer); order.setProperty(prop);
        order.setAmount(amount); order.setStatus(PaymentStatus.PENDING);
        order.setQrCodeUrl("/mock-qr/" + order.getOutTradeNo());
        paymentRepo.save(order);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/mock-pay")
    public ResponseEntity<?> mockPay(@PathVariable Long id) {
        PaymentOrder order = paymentRepo.findById(id).orElseThrow();
        order.setStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        paymentRepo.save(order);
        order.getAppointment().setStatus(Appointment.Status.CONFIRMED);
        appointmentRepo.save(order.getAppointment());
        return ResponseEntity.ok("Payment successful");
    }
}"""

fig2,ax2=plt.subplots(figsize=(12,7)); ax2.set_xlim(0,12); ax2.set_ylim(0,7); ax2.axis('off')
fig2.patch.set_facecolor('#1E1E2E'); ax2.set_facecolor('#1E1E2E')
tb2=mp.FancyBboxPatch((0,6.6),12,0.4,boxstyle='round,pad=0',facecolor='#313244',ec='none',zorder=2)
ax2.add_patch(tb2)
ax2.text(0.3,6.8,'PaymentController.java',fontsize=9,color='#CDD6F4',va='center',family='Courier New')
for i,ln in enumerate(pay_code.strip().split('\n')):
    y=6.4-(i*0.27)
    if y<0.1: break
    ax2.text(0.2,y,ln,fontsize=7.2,color='#CDD6F4',va='center',family='Courier New',zorder=3)
plt.tight_layout(pad=0)
plt.savefig(os.path.join(OUT,'code-payment.png'),dpi=180,bbox_inches='tight',facecolor='#1E1E2E'); plt.close()
print('code-payment.png done')

