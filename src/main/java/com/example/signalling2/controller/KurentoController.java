package com.example.signalling2.controller;

import com.example.signalling2.controller.dto.Response.EndpointResponseDto;
import com.example.signalling2.controller.dto.Response.PipelineResponseDto;
import com.example.signalling2.common.ResponseDto;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class KurentoController {
    private final KurentoClient kurento;
    private final RoomService roomService;
    private final MediaService mediaService;
    @GetMapping("/pipelines")
    public ResponseEntity<Object> getPipelines() {
       List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
       List<PipelineResponseDto> piplineList = new ArrayList<>();
       for (MediaPipeline pipeline : pipelines) {
           PipelineResponseDto pipelineResponseDto = new PipelineResponseDto(pipeline.getName(), pipeline.getId());
           piplineList.add(pipelineResponseDto);
       }
       return ResponseDto.ok(piplineList);
    }

    @GetMapping("/endpoints/{email}")
    public ResponseEntity<Object> getChildren(@PathVariable("email") String email) {
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

    @GetMapping("/pipelines/exception")
    public ResponseEntity<Object> getUnusedPipelines() {
        List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
        ArrayList<String> roomList = roomService.findAllPipelines();
        List<PipelineResponseDto> piplineList = new ArrayList<>();
        for (MediaPipeline pipeline : pipelines) {
            if (!roomList.contains(pipeline.getId())) {
                PipelineResponseDto pipelineResponseDto = new PipelineResponseDto(pipeline.getName(), pipeline.getId());
                piplineList.add(pipelineResponseDto);
            }
        }
        return ResponseDto.ok(piplineList);
    }

    @DeleteMapping("/pipelines/exception")
    public ResponseEntity removePipelines() {
        List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
        ArrayList<String> roomList = roomService.findAllPipelines();
        for (MediaPipeline pipeline : pipelines) {
            if (!roomList.contains(pipeline.getId())) {
                pipeline.release();
            }
        }
        return ResponseDto.noContent();
    }

    @GetMapping("/cpu")
    public ResponseEntity<Object> cpuUsage() {
        String cpuPercent = mediaService.cpuUsage() + "%";
        return ResponseDto.ok(cpuPercent);
    }

    @GetMapping("/memory")
    public ResponseEntity<Object> memUsage() {
        String memPercent = mediaService.memUsage() + "%";
        return ResponseDto.ok(memPercent);
    }
}
