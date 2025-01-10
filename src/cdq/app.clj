(ns cdq.app
  (:require [gdl.app :as app]
            [cdq.create :as create]
            [cdq.render :as render])
  (:gen-class))

; => TODO ui is not disposed - this is the VisUI skin and is global state there - so just define it in my context ?
; then it doesn't know its disposed ? omg ...
;:gdl.context/ui-skin (com.kotcrab.vis.ui.VisUI/getSkin)

; TODO tiled-map also dispose if new game state add
; this also if world restarts !!

; => the comments are the problem!

(defn -main []
  (app/start create/game render/game))

; separate concerns - create -> all the instances - rest of code w. protocols (also tiledmap drawer etc.)
; * draw
; * update
