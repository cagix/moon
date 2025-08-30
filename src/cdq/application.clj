(ns cdq.application)

(def state (atom nil))

; usage: debug, dev, user-interface swap!'s
; -> pass to stage ... with warning?
; InputProcessor cant pass at act stage
; -> need the atom itself ?
; -> can deref it ? but then in render loop outdated?

; Only usages:
; * dev/debug
; * editor (database)           - belongs outside the game as javafx thingy?
; * dev-menu (reset game state) - belongs outside the game as javafx thingy?
