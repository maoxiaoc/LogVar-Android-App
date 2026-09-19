# LogVar notices

## Upstream project

The server implementation in this repository is based on and contains modifications to [`huangxd-/danmu_api`](https://github.com/huangxd-/danmu_api). The upstream project is distributed under the GNU Affero General Public License v3.0. The applicable license text is in [`LICENSE`](LICENSE). Changes made in this repository are part of the corresponding source for the modified server.

Logvar Android App is an independent, unofficial client application. The current release uses the upstream project's LogVar name and icon for project identification. It is not an official release of the upstream project.

## Design reference

The UI design was informed by [`lnkiai/m3e-canvas`](https://github.com/lnkiai/m3e-canvas). Its MIT license applies to that project; this repository does not claim ownership of its code or artwork.

## Direct server dependencies

The embedded server uses the following direct packages. Their notices and license texts remain the responsibility of each package's respective author:

| Package | License |
| --- | --- |
| `@dan-uni/dan-any` | LGPL-3.0-or-later |
| `brotli` | MIT |
| `chokidar` | MIT |
| `dotenv` | BSD-2-Clause |
| `esbuild` | MIT |
| `https-proxy-agent` | MIT |
| `node-fetch` | MIT |
| `opencc-js` | MIT AND Apache-2.0 |
| `pako` | MIT AND Zlib |
| `redis` | MIT |

The lock files identify the exact versions used for a build. Transitive dependencies may carry additional notices; do not remove their license files when preparing a distributable build.

## Third-party services and data

The source adapters query third-party websites. Users are responsible for following the applicable platform rules and copyright requirements. Logvar is intended for personal learning and technical exchange; it does not grant permission to redistribute third-party content or operate a public content service.
