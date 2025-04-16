package site.linkverse.back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.linkverse.back.enums.MediaType;
import site.linkverse.back.enums.SearchType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {
    private String keyword;
    private SearchType searchType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private MediaType mediaType;
    private Integer page;
    private Integer size;
}