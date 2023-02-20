package com.example.signalling2.controller;

import com.example.signalling2.dto.Response.EndpointResponseDto;
import com.example.signalling2.dto.Response.PipelineResponseDto;
import com.example.signalling2.dto.Response.ResponseDto;
import com.example.signalling2.dto.Response.RoomResponseDto;
import com.example.signalling2.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class KurentoController {
    private final KurentoClient kurento;
    private final RoomService roomService;
    @GetMapping("/pipelines")
    public ResponseEntity<Object> getPipelines() {
       List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
       List<PipelineResponseDto> piplineList = new ArrayList<>();
       System.out.println("pipeline num: " + pipelines.size());
       for (MediaPipeline pipeline : pipelines) {
           PipelineResponseDto pipelineResponseDto = new PipelineResponseDto(pipeline.getId(), pipeline.getName());
           piplineList.add(pipelineResponseDto);
       }
       return ResponseDto.ok(piplineList);
    }

    @GetMapping("/pipeline/{email}")
    public ResponseEntity<Object> getPipeline(@PathVariable("email") String email) {
        String pipelineId = roomService.findById(email).getMediaPipeline();
        MediaPipeline pipeline = kurento.getById(pipelineId, MediaPipeline.class);
        List<MediaObject> children = pipeline.getChildren();
        List<EndpointResponseDto> endpointList = new ArrayList<>();
        for (MediaObject endpoint : children) {
            EndpointResponseDto endpointResponseDto = new EndpointResponseDto(endpoint.getId(), endpoint.getName());
            endpointList.add(endpointResponseDto);
        }
        return ResponseDto.ok(endpointList);
    }

    @GetMapping("/clear") // notice: 일단은 getmapping
    public ResponseEntity<Object> removePipelines() {
        List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
        System.out.println("pipeline num: " + pipelines.size());
        for (MediaPipeline pipeline : pipelines) {
            pipeline.release();
        }
        return ResponseDto.ok(kurento.getServerManager().getPipelines());
    }

}
