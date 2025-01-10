(ns cdq.app
  (:require [gdl.app :as app]
            [cdq.game :as game])
  (:gen-class))

; => TODO ui is not disposed - this is the VisUI skin and is global state there - so just define it in my context ?
; TODO tiled-map also dispose if new game state add
; this also if world restarts !!

; => the comments are the problem!

(defn -main []
  (app/start game/create!
             game/render!))
