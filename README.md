# ZipBackup

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/okocraft/ZipBackup)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/okocraft/ZipBackup/Java%20CI)
![GitHub](https://img.shields.io/github/license/okocraft/ZipBackup)

ワールドやプラグインを自動でバックアップする Paper プラグインです。

A Paper plugin for automatic backup of worlds and plugins.

## 動作環境 / Requirements

- Java 11+
- Paper 1.16.5 (after 'adventure update') or its fork

## コマンド / Command

引数に対してタブ補完が利用できます。

- `/zipbackup` - ヘルプの表示 (略: `zb` `zbu` `zbackup`)
- `/zb backup plugin` - プラグインフォルダーをバックアップする
- `/zb backup world` - 設定で除外した名前以外のワールドをすべてバックアップする
- `/zb backup world {world-name}` - 指定したワールドをバックアップする
- `/zb purge` - 期限切れのバックアップを削除する
- `/zb reload` - config.yml を再読み込みする

---

Tab completion is available for the arguments.

- `/zipbackup` - Showing helps (Aliases: `zb` `zbu` `zbackup`)
- `/zb backup plugin` - Backup the `plugins` folder
- `/zb backup world` - Backup all worlds unless names are excluded in configuration
- `/zb backup world {world-name}` - Backup a specified world
- `/zb purge` - Delete an expired backups.
- `/zb reload` - Reload `config.yml`

## 設定 / Configurations

```yaml
backup:
  directory: "" # 保存先ディレクトリ。空設定で `plugins/ZipBackup/backups`
  zip-compression-level: "NORMAL" # 圧縮レベル: FASTEST, NORMAL, ULTRA or https://javadoc.io/doc/net.lingala.zip4j/zip4j/latest/net/lingala/zip4j/model/enums/CompressionLevel.html
  plugin: # プラグインフォルダーのバックアップ設定
    interval: 60 # バックアップ間隔 (分) 0以下でバックアップしない
    backup-after-startup: true # サーバースタート直後にバックアップするか
    ignore-jar-files: false # .jar で終わるファイルを無視するか
    exclude-folders: [] # 除外するフォルダー名 / ファイル名
  world: # ワールドのバックアップ設定
    interval: 60 # バックアップ間隔 (分) 0以下でバックアップしない 
    backup-after-startup: true # サーバースタート直後にバックアップするか
    exclude-worlds: [] # 除外するワールドの名前
  purge: # 自動削除設定
    check-interval: 720 # 期限切れのバックアップを確認する間隔 (分) 0以下で起動後実行しない。
    purge-after-startup: true # サーバースタート直後に確認するか
    expiration-days: 7 # バックアップの期限 (日)
    maximum-files: 10 # バックアップファイルの最大数
```

```yaml
backup:
  directory: "" # Destination directory. If empty, `plugins/ZipBackup/backups`.
  zip-compression-level: "NORMAL" # Compression level: FASTEST, NORMAL, ULTRA or https://javadoc.io/doc/net.lingala.zip4j/zip4j/latest/net/lingala/zip4j/model/enums/CompressionLevel.html
  plugin: # Backup settings for the plugin folder
    interval: 60 # Backup interval (minutes), no backup less than 0
    backup-after-startup: true # Backup when server has been started
    ignore-jar-files: false # Ignore files that end with .jar
    exclude-folders: [] # Folder/file name to exclude
  world: # Backup settings for worlds
    interval: 60 # Backup interval (minutes) no backup less than 0
    backup-after-startup: true # Backup when server has been started
    exclude-worlds: [] # Name of worlds to exclude
  purge: # Automatic deletion setting
    check-interval: 720 # Interval to check for expired backups (minutes), no purge less than 0 
    purge-after-startup: true # Purge when server has been started
    expiration-days: 7 # Backup expiration (days)
    maximum-files: 10 # Maximum number of backup files
```

## ライセンス / LICENSE

This project is under the GPL-3.0. Please see [LICENSE](LICENSE) for more info.

Copyright © 2021, OKOCRAFT and Siroshun09
