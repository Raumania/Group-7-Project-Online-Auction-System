package auction_system.server.controller;

import auction_system.common.dto.AIDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.service.AIService;
import auction_system.server.util.GsonUtil;

public class AIController implements RequestHandler {
    private final AIService aiService = AIService.getInstance();

    public Response handle(Request request) {
        Action action = request.getAction();
        AIDTO aiDTO = GsonUtil.fromJson(request.getData(), AIDTO.class);
        String aiReply = aiService.chat(aiDTO.getPrompt(), aiDTO.getImageBase64());
        return new Response(Status.SUCCESS, action, aiReply, "AI reply");
    }
}
