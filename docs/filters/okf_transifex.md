# Transifex Filter

The Transifex Filter processes online [Transifex](http://www.transifex.org) translation projects by reading a local `.txp` project file. It connects to a Transifex host, downloads PO-based translation resources, and processes them as standard PO filter events. When used with a filter-writer, modified PO files are pushed back to the Transifex project.

> **Note:** This filter is BETA.

## How It Works

1. The filter reads a local `.txp` project file.
2. If the file contains no resources, it connects to Transifex and fetches the full resource list. Otherwise, it uses the resources already listed.
3. For each PO resource matching the source locale, it downloads the translation file for the target language (or creates one with empty entries if none exists).
4. It processes the downloaded PO file, sending standard filter events.
5. If a filter-writer is used, modified PO files are pushed back to Transifex.

## Parameters

#### Open Project File Before Processing
Opens the `.txp` project file in an editor dialog before processing begins, allowing you to review and modify project settings (host, credentials, resource selection) interactively.

#### Host URL
The URL of the Transifex server where your project is hosted (e.g., `http://www.transifex.net/`).

#### Username
Your Transifex username for authenticating with the host.

#### Password
The password for the given Transifex user. Note that credentials are stored unencrypted in the `.txp` file.

#### Project ID
The identifier of the Transifex project you want to work with (e.g., `myproject`).

#### Protect Approved Entries
Marks all translation entries that are not empty and not fuzzy with a non-translatable flag, preventing them from being modified during processing.

> **Warning:** Transifex currently loses fuzzy flags and their translations when downloading a PO file uploaded with fuzzy entries. The only safe round-trip approach is to not label entries as fuzzy, which may cause resources to appear 100% translated even when translations are not final.

#### Source Locale
Fall-back source locale code (e.g., `en`). In most situations the source locale is driven by the calling application; this value is only used as a fall-back.

#### Target Locale
Fall-back target locale code (e.g., `fr`). In most situations the target locale is driven by the calling application; this value is only used as a fall-back.

## Project File Format

The `.txp` file is a UTF-8 text file with the following format:

```properties
host = <hostURL>
user = <username>
password = <password>
projectId = <projectId>
protectApproved = yes|no
sourceLocale = <localeCode>
targetLocale = <localeCode>
<resourceId1>	yes|no
<resourceId2>	yes|no
```

- Each entry can be omitted (defaults will be used).
- If the `yes|no` selection flag is not present for a resource, it defaults to *selected*.
- Lines with only whitespace and lines starting with `#` are ignored.

### Example

```properties
host=http://www.transifex.net
user=johndoe
password=xrt34@asf
projectId=myproject
protectApproved=yes
# Fall-back values for the source and target locales:
sourceLocale=en
targetLocale=fr
# Resources
myfile1xmlpo	yes
myfile2odtpo	no
```

## Limitations

- This filter is **BETA**.
- Transifex loses fuzzy flags and their corresponding translations when downloading a PO file uploaded with fuzzy entries. Round-trips without losing target text require not labeling entries as fuzzy, which may incorrectly show resources as 100% translated.
- Only PO-based files in Transifex are handled — TS files are not supported.
- Due to pipeline mechanism limitations, this filter cannot be used with steps that re-write different output corresponding to the resource, other than the **Filter Events to Raw Document Step**. It will not work properly with the **Rainbow Translation Kit Creation Step**.

## Notes

- The source and target locale values in the `.txp` file are fall-back values only; in most situations these are driven by the calling application.
- The filter-writer pushes modified PO files back to Transifex using the same credentials specified in the project file.
- Resource selection can be managed either by editing the `.txp` file directly or by using the **Open project file before processing** option to launch the editor dialog.
