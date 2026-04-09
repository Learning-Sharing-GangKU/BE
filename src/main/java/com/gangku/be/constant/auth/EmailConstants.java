package com.gangku.be.constant.auth;

public class EmailConstants {

    private EmailConstants() {}

    public static final String VERIFICATION_PATH = "/api/v1/auth/email/verification/start?token=";

    public static final String VERIFICATION_SUBJECT = "[GangKU] 이메일 인증을 완료해주세요.";

    public static final String VERIFICATION_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko">
              <body style="margin:0; padding:0; background-color:#f5f7fb; font-family:Arial, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif; color:#1f2937;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f5f7fb; padding:32px 0;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px; background-color:#ffffff; border-radius:20px; overflow:hidden; box-shadow:0 10px 30px rgba(67,56,202,0.12);">
            
                        <tr>
                            <td align="center" bgcolor="#4338CA" style="background-color:#4338CA; padding:36px 24px; text-align:center;">
                            <div style="font-size:30px; font-weight:800; color:#ffffff; letter-spacing:-0.5px;">
                              GangKU
                            </div>
                            <div style="margin-top:10px; font-size:14px; color:rgba(255,255,255,0.92);">
                              모임에서 시작되는 새로운 연결
                            </div>
                          </td>
                        </tr>
            
                        <tr>
                          <td style="padding:40px 32px;">
                            <h1 style="margin:0 0 16px; font-size:26px; line-height:1.4; color:#111827;">
                              이메일 인증을 완료해주세요
                            </h1>
            
                            <p style="margin:0 0 14px; font-size:16px; line-height:1.8; color:#374151;">
                              안녕하세요, <strong style="color:#4338CA;">GangKU</strong>입니다.<br>
                              회원가입을 완료하려면 아래 버튼을 눌러 이메일 인증을 진행해주세요.
                            </p>
            
                            <p style="margin:0 0 28px; font-size:15px; line-height:1.8; color:#6b7280;">
                              버튼을 클릭하면 인증이 완료되며, 이후 다시 회원가입 절차를 이어서 진행할 수 있습니다.
                            </p>
            
                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:28px auto;">
                              <tr>
                                <td align="center" style="border-radius:12px; background-color:#4338CA;">
                                  <a href="%s" target="_blank"
                                     style="display:inline-block; padding:16px 34px; font-size:16px; font-weight:700; color:#ffffff; text-decoration:none; border-radius:12px;">
                                    이메일 인증하기
                                  </a>
                                </td>
                              </tr>
                            </table>
            
                            <div style="margin:0 0 24px; padding:16px 18px; background-color:#EEF2FF; border:1px solid #C7D2FE; border-radius:12px; font-size:14px; line-height:1.7; color:#3730A3;">
                              ⏰ 이 인증 링크는 <strong>%d분</strong> 동안 유효합니다.
                            </div>
            
                            <hr style="border:none; border-top:1px solid #E5E7EB; margin:28px 0;" />
            
                            <p style="margin:0; font-size:13px; line-height:1.7; color:#9CA3AF;">
                              본 메일은 발신 전용입니다. 본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.
                            </p>
                          </td>
                        </tr>
            
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;
}
