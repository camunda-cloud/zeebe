package main

import (
	"errors"
	"fmt"
	"github.com/camunda/camunda/c8run/internal/archive"
	"os"
	"path/filepath"
	"runtime"
	"runtime/debug"
)

func Clean(camundaVersion string, elasticsearchVersion string) {
	os.RemoveAll("elasticsearch-" + elasticsearchVersion)
	os.RemoveAll("camunda-zeebe-" + camundaVersion)

	logFiles := []string{"camunda.log", "connectors.log", "elasticsearch.log"}
	for _, logFile := range logFiles {
		_, err := os.Stat(filepath.Join("log", logFile))
		if !errors.Is(err, os.ErrNotExist) {
			os.Remove(filepath.Join("log", logFile))
		}
	}
}

func downloadAndExtract(filePath, url, extractDir string, authToken string, extractFunc func(string, string) error) error {
	err := archive.DownloadFile(filePath, url, authToken)
	if err != nil {
		return fmt.Errorf("downloadAndExtract: failed to download file at url %s\n%w\n%s", url, err, debug.Stack())
	}

	_, err = os.Stat(extractDir)
	if errors.Is(err, os.ErrNotExist) {
		err = extractFunc(filePath, ".")
		if err != nil {
			return fmt.Errorf("downloadAndExtract: failed to extract from archive at %s\n%w\n%s", filePath, err, debug.Stack())
		}
	}
	return nil
}

func PackageWindows(camundaVersion string, elasticsearchVersion string, connectorsVersion string, camundaReleaseTag string) error {
	elasticsearchUrl := "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-" + elasticsearchVersion + "-windows-x86_64.zip"
	elasticsearchFilePath := "elasticsearch-" + elasticsearchVersion + ".zip"
	camundaFilePath := "camunda-zeebe-" + camundaVersion + ".zip"
	camundaUrl := "https://github.com/camunda/camunda/releases/download/" + camundaReleaseTag + "/" + camundaFilePath
	connectorsFilePath := "connector-runtime-bundle-" + connectorsVersion + "-with-dependencies.jar"
	connectorsUrl := "https://repository.nexus.camunda.cloud/content/groups/internal/io/camunda/connector/connector-runtime-bundle/" + connectorsVersion + "/" + connectorsFilePath
	authToken := os.Getenv("GH_TOKEN")

	javaArtifactsUser := os.Getenv("JAVA_ARTIFACTS_USER")
	javaArtifactsPassword := os.Getenv("JAVA_ARTIFACTS_PASSWORD")

	if javaArtifactsUser == "" || javaArtifactsPassword == "" {
		return fmt.Errorf("PackageWindows: JAVA_ARTIFACTS_USER or JAVA_ARTIFACTS_PASSWORD env vars are not set")
	}
        javaArtifactsToken := "Basic " + base64.StdEncoding.EncodeToString([]byte(javaArtifactsUser+":"+javaArtifactsPassword))

	Clean(camundaVersion, elasticsearchVersion)

	err := downloadAndExtract(elasticsearchFilePath, elasticsearchUrl, "elasticsearch-"+elasticsearchVersion, "", archive.UnzipSource)
	if err != nil {
		return fmt.Errorf("PackageWindows: failed to fetch elasticsearch: %w\n%s", err, debug.Stack())
	}

	err = downloadAndExtract(camundaFilePath, camundaUrl, "camunda-zeebe-"+camundaVersion, authToken, archive.UnzipSource)
	if err != nil {
		return fmt.Errorf("PackageWindows: failed to fetch camunda: %w\n%s", err, debug.Stack())
	}

	err = downloadAndExtract(connectorsFilePath, connectorsUrl, connectorsFilePath, javaArtifactsToken, func(_, _ string) error { return nil })
	if err != nil {
		return fmt.Errorf("PackageWindows: failed to fetch connectors: %w\n%s", err, debug.Stack())
	}

	os.Chdir("..")
	filesToArchive := []string{
		filepath.Join("c8run", "README.md"),
		filepath.Join("c8run", "connectors-application.properties"),
		filepath.Join("c8run", connectorsFilePath),
		filepath.Join("c8run", "elasticsearch-"+elasticsearchVersion),
		filepath.Join("c8run", "custom_connectors"),
		filepath.Join("c8run", "configuration"),
		filepath.Join("c8run", "c8run.exe"),
		filepath.Join("c8run", "endpoints.txt"),
		filepath.Join("c8run", "log"),
		filepath.Join("c8run", "camunda-zeebe-"+camundaVersion),
		filepath.Join("c8run", "package.bat"),
	}
	err = archive.ZipSource(filesToArchive, filepath.Join("c8run", "camunda8-run-"+camundaVersion+"-windows-x86_64.zip"))
	if err != nil {
		return fmt.Errorf("PackageWindows: failed to create c8run package %w\n%s", err, debug.Stack())
	}
	os.Chdir("c8run")
	return nil
}

func PackageUnix(camundaVersion string, elasticsearchVersion string, connectorsVersion string, camundaReleaseTag string) error {
	var architecture string
	if runtime.GOARCH == "amd64" {
		architecture = "x86_64"
	} else if runtime.GOARCH == "arm64" {
		architecture = "aarch64"
	}

	elasticsearchUrl := "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-" + elasticsearchVersion + "-" + runtime.GOOS + "-" + architecture + ".tar.gz"
	elasticsearchFilePath := "elasticsearch-" + elasticsearchVersion + ".tar.gz"
	camundaFilePath := "camunda-zeebe-" + camundaVersion + ".tar.gz"
	camundaUrl := "https://github.com/camunda/camunda/releases/download/" + camundaReleaseTag + "/" + camundaFilePath
	connectorsFilePath := "connector-runtime-bundle-" + connectorsVersion + "-with-dependencies.jar"
	connectorsUrl := "https://repository.nexus.camunda.cloud/content/groups/internal/io/camunda/connector/connector-runtime-bundle/" + connectorsVersion + "/" + connectorsFilePath

	authToken := os.Getenv("GH_TOKEN")

	javaArtifactsUser := os.Getenv("JAVA_ARTIFACTS_USER")
	javaArtifactsPassword := os.Getenv("JAVA_ARTIFACTS_PASSWORD")

	if javaArtifactsUser == "" || javaArtifactsPassword == "" {
		return fmt.Errorf("PackageUnix: JAVA_ARTIFACTS_USER or JAVA_ARTIFACTS_PASSWORD env vars are not set")
	}
        javaArtifactsToken := "Basic " + base64.StdEncoding.EncodeToString([]byte(javaArtifactsUser+":"+javaArtifactsPassword))

	Clean(camundaVersion, elasticsearchVersion)

	err := downloadAndExtract(elasticsearchFilePath, elasticsearchUrl, "elasticsearch-"+elasticsearchVersion, "", archive.ExtractTarGzArchive)
	if err != nil {
		return fmt.Errorf("PackageUnix: failed to fetch elasticsearch %w\n%s", err, debug.Stack())
	}

	err = downloadAndExtract(camundaFilePath, camundaUrl, "camunda-zeebe-"+camundaVersion, authToken, archive.ExtractTarGzArchive)
	if err != nil {
		return fmt.Errorf("PackageUnix: failed to fetch camunda %w\n%s", err, debug.Stack())
	}

	err = downloadAndExtract(connectorsFilePath, connectorsUrl, connectorsFilePath, javaArtifactsToken, func(_, _ string) error { return nil })
	if err != nil {
		return fmt.Errorf("PackageUnix: failed to fetch connectors %w\n%s", err, debug.Stack())
	}

	os.Chdir("..")
	filesToArchive := []string{
		filepath.Join("c8run", "README.md"),
		filepath.Join("c8run", "connectors-application.properties"),
		filepath.Join("c8run", connectorsFilePath),
		filepath.Join("c8run", "elasticsearch-"+elasticsearchVersion),
		filepath.Join("c8run", "custom_connectors"),
		filepath.Join("c8run", "configuration"),
		filepath.Join("c8run", "c8run"),
		filepath.Join("c8run", "endpoints.txt"),
		filepath.Join("c8run", "log"),
		filepath.Join("c8run", "camunda-zeebe-"+camundaVersion),
		filepath.Join("c8run", "start.sh"),
		filepath.Join("c8run", "shutdown.sh"),
		filepath.Join("c8run", "package.sh"),
	}
	outputArchive, err := os.Create(filepath.Join("c8run", "camunda8-run-"+camundaVersion+"-"+runtime.GOOS+"-"+architecture+".tar.gz"))
	if err != nil {
		return fmt.Errorf("PackageUnix: failed to create empty archive file %w\n%s", err, debug.Stack())
	}
	err = archive.CreateTarGzArchive(filesToArchive, outputArchive)
	if err != nil {
		return fmt.Errorf("PackageUnix: failed to fill camunda archive %w\n%s", err, debug.Stack())
	}
	os.Chdir("c8run")
	return nil
}
