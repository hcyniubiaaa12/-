package com.guide.rag.dto;

/**
 * 候选科室（由 chat 层从 kb 的 dept 表读取后传入）。
 * rag 层不感知业务状态，科室范围由调用方给定——只推荐这里列出的科室。
 */
public record DeptOption(String id, String name) {
}
