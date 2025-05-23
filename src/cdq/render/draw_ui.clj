(ns cdq.render.draw-ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (reset! (.ctx stage) ctx)
  (ui/draw! stage)
  ; we need to set nil as input listeners
  ; are updated outside of render
  ; inside lwjgl3application code
  ; so it has outdated context
  ; => maybe context should be an immutable data structure with mutable fields?
  #_(reset! (.ctx stage) nil)
  nil)
