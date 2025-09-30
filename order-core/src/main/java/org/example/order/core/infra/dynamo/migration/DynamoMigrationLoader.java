package org.example.order.core.infra.dynamo.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.dynamo.migration.model.MigrationFile;
import org.example.order.core.infra.dynamo.migration.model.SeedFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DynamoMigrationLoader
 * ------------------------------------------------------------------------
 * - /resources/dynamodb/migration(Vn), /resources/dynamodb/seed(Vn) 구조 지원
 * - "최신 버전(Vn)만" 스캔하여 같은 버전 내 모든 파일을 병합 로드
 * - (예) V2__*.json 파일이 여러 개면 모두 읽어 tables/items를 합친다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoMigrationLoader {

    private static final Pattern VERSION_PATTERN = Pattern.compile(".*/V(\\d+)__.*\\.json$");
    private final ResourcePatternResolver resolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<MigrationFile> loadLatestMigration(String locationPattern) {
        List<Resource> latest = findLatestVersionResources(locationPattern);

        if (latest.isEmpty()) {
            return Optional.empty();
        }

        try {
            MigrationFile merged = new MigrationFile();
            for (Resource r : latest) {
                MigrationFile mf = objectMapper.readValue(r.getInputStream(), MigrationFile.class);

                if (mf.getTables() != null) {
                    merged.getTables().addAll(mf.getTables());
                }

                log.info("[DynamoMigration] Loaded latest migration {}", safeName(r));
            }

            return Optional.of(merged);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load latest migration", e);
        }
    }

    public Optional<List<SeedFile>> loadLatestSeeds(String locationPattern) {
        List<Resource> latest = findLatestVersionResources(locationPattern);

        if (latest.isEmpty()) {
            return Optional.empty();
        }

        try {
            List<SeedFile> list = new ArrayList<>();

            for (Resource r : latest) {
                SeedFile sf = objectMapper.readValue(r.getInputStream(), SeedFile.class);
                list.add(sf);

                log.info("[DynamoSeed] Loaded latest seed {}", safeName(r));
            }

            return Optional.of(list);
        } catch (Exception e) {
            log.info("[DynamoSeed] No latest seeds found at {}", locationPattern);

            return Optional.empty();
        }
    }

    /* ================= 내부 유틸 ================= */

    private List<Resource> findLatestVersionResources(String base) {
        try {
            Resource[] resources = resolver.getResources(
                    base.endsWith(".json") ? base : (base + "/V*__*.json")
            );

            Map<Integer, List<Resource>> byVer = new HashMap<>();
            int max = -1;

            for (Resource r : resources) {
                if (!r.isReadable()) continue;
                int v = versionOf(r);
                max = Math.max(max, v);
                byVer.computeIfAbsent(v, k -> new ArrayList<>()).add(r);
            }

            return max >= 0 ? byVer.getOrDefault(max, Collections.emptyList()) : Collections.emptyList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan migration resources at: " + base, e);
        }
    }

    private static int versionOf(Resource r) {
        try {
            String path = r.getURL().toString();
            Matcher m = VERSION_PATTERN.matcher(path);

            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static String safeName(Resource r) {
        try {
            return r.getFilename();
        } catch (Exception e) {
            return String.valueOf(r);
        }
    }
}
