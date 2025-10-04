(ns cdq.ctx.dispose
  (:require [clojure.disposable :refer [dispose!]]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/vis-ui
                   ctx/world]}]
  (dispose! audio)
  (dispose! graphics)
  (dispose! vis-ui)
  (dispose! world))
