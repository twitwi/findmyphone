
- Handle deep links to setup SMS format from a web page, e.g. `capnn://short=twitwi,cap_nn,22eb149,1660881600` `capnn://short=twitwi,22eb149,1660881600` or maybe rather a generic `findmyphone://format=......`
- Invert-sound-mode: do not play any sound on SMS reception, except if the received SMS is not a "GPS" query
- Estimate current speed (and time to arrival if the destination is know (how?)) -> moved to cap_nn companion website
- New "push" mode: send an SMS every *minutes on *kilometers
- Invert-sound-mode: do not play any sound on SMS reception, except if the received SMS is not a "GPS" query
- Estimate current speed (and time to arrival if the destination is know (how?)) -> outsourced to the web-based https://github.com/twitwi/cap_nn
