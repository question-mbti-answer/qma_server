package com.l1mit.qma_server.domain.member;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.l1mit.qma_server.domain.member.domain.Member;
import com.l1mit.qma_server.domain.member.domain.Oauth2Entity;
import com.l1mit.qma_server.domain.member.domain.enums.SocialProvider;
import com.l1mit.qma_server.domain.member.dto.SignInRequest;
import com.l1mit.qma_server.global.auth.AuthService;
import com.l1mit.qma_server.global.auth.oauth.dto.IdTokenResponse;
import com.l1mit.qma_server.global.common.domain.MbtiEntity;
import com.l1mit.qma_server.global.exception.ErrorCode;
import com.l1mit.qma_server.global.exception.QmaApiException;
import com.l1mit.qma_server.setting.docs.RestDocsControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = MemberController.class)
public class MemberControllerTest extends RestDocsControllerTest {

    @MockBean
    MemberService memberService;
    @MockBean
    AuthService authService;

    @Nested
    @DisplayName("signIn 메소드는")
    class signIn {

        SignInRequest signInRequest = SignInRequest.builder()
                .code("authCode")
                .redirectUri("http://redirect.uri")
                .build();
        IdTokenResponse idTokenResponse = IdTokenResponse.builder()
                .idToken("IdToken")
                .build();

        @Test
        @DisplayName("성공한다.")
        void success() throws Exception {
            //given
            String PROVIDER = "PROVIDER";
            given(authService.signIn(PROVIDER, signInRequest)).willReturn(idTokenResponse);

            //when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/{PROVIDER}/sign-in", PROVIDER)
                            .content(toJson(signInRequest))
                            .contentType(MediaType.APPLICATION_JSON)
            );

            //then
            resultActions
                    .andExpect(status().isOk())
                    .andDo(document("sign-in",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint()),
                                    resource(ResourceSnippetParameters.builder()
                                            .tag("유저")
                                            .summary("로그인")
                                            .pathParameters(
                                                    parameterWithName("PROVIDER")
                                                            .description("소셜 서비스")
                                            )
                                            .requestFields(
                                                    fieldWithPath("code").description("code"),
                                                    fieldWithPath("redirect_uri").description(
                                                            "redirect_uri")
                                            )
                                            .responseFields(
                                                    fieldWithPath("data").description("응답 데이터"),
                                                    fieldWithPath("data.id_token").description("토큰"),
                                                    fieldWithPath("message").description("오류 메세지"),
                                                    fieldWithPath("timestamp").description("타임 스탬프")
                                            )
                                            .requestSchema(Schema.schema("SignInRequest"))
                                            .responseSchema(Schema.schema("IdTokenResponse"))

                                            .build()
                                    )
                            )
                    );
        }

        @Test
        @DisplayName("실패한다.")
        void fail() throws Exception {
            //given
            String PROVIDER = "NONE";
            given(authService.signIn(PROVIDER, signInRequest)).willThrow(new QmaApiException(
                    ErrorCode.THIRD_PARTY_API_EXCEPTION));
            //when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/{PROVIDER}/sign-in", PROVIDER)
                            .content(toJson(signInRequest))
                            .contentType(MediaType.APPLICATION_JSON));

            //then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andDo(document("sign-in-fail",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint()),
                                    resource(ResourceSnippetParameters.builder()
                                            .tag("유저")
                                            .summary("로그인")
                                            .pathParameters(
                                                    parameterWithName("PROVIDER").description("소셜 서비스")
                                            )
                                            .requestFields(
                                                    fieldWithPath("code").description("code"),
                                                    fieldWithPath("redirect_uri").description(
                                                            "redirect_uri")
                                            )
                                            .responseFields(
                                                    fieldWithPath("data").description("응답 데이터"),
                                                    fieldWithPath("message").description("오류 메세지"),
                                                    fieldWithPath("timestamp").description("타임 스탬프")
                                            )
                                            .requestSchema(Schema.schema("SignInRequest"))
                                            .responseSchema(Schema.schema("IdTokenResponse"))
                                            .build()
                                    )
                            )
                    );
        }
    }

    @Nested
    @DisplayName("myInfo 메소드는")
    class myInfo {

        @Test
        @DisplayName("성공한다.")
        void success() throws Exception {
            //given
            Long memberId = 1L;
            Oauth2Entity oauth2Entity = Oauth2Entity.builder()
                    .accountId("1")
                    .socialProvider(SocialProvider.KAKAO)
                    .build();
            Member member = Member.builder()
                    .oauth2Entity(oauth2Entity)
                    .build();
            member.updateMbtiEntity(MbtiEntity.builder()
                    .attitude("I")
                    .perception("S")
                    .decision("T")
                    .lifestyle("J")
                    .build());

            given(memberService.findById(memberId)).willReturn(member);
            //when
            ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/my-info")
                    .header("Authorization", "id_token")
                    .header("Provider", "provider")
                    .contentType(MediaType.APPLICATION_JSON));
            //then
            resultActions
                    .andExpect(status().isOk())
                    .andDo(document("my-info",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint()),
                                    resource(ResourceSnippetParameters.builder()
                                            .tag("유저")
                                            .summary("정보 불러오기")
                                            .requestHeaders(
                                                    headerWithName("Authorization").description("인증 토큰"),
                                                    headerWithName("Provider").description("인증 소셜 서비스 이름")
                                            )
                                            .responseFields(
                                                    fieldWithPath("data").description("응답 데이터"),
                                                    fieldWithPath("data.provider").description(
                                                            "가입한 소셜 서비스"),
                                                    fieldWithPath("data.mbti").description("유저의 mbti"),
                                                    fieldWithPath("data.mbti.attitude").description("I, E"),
                                                    fieldWithPath("data.mbti.perception").description(
                                                            "S, N"),
                                                    fieldWithPath("data.mbti.decision").description("T, F"),
                                                    fieldWithPath("data.mbti.lifestyle").description(
                                                            "P, J"),
                                                    fieldWithPath("message").description("오류 메세지"),
                                                    fieldWithPath("timestamp").description("타임 스탬프")
                                            )
                                            .responseSchema(Schema.schema("MemberInfoResponse"))
                                            .build()
                                    )
                            )
                    );
        }
    }


}
