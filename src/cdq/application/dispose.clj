(ns cdq.application.dispose
  (:require [gdl.utils.disposable :as disposable]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/vis-ui
                   ctx/world]}]
  (disposable/dispose! audio)
  (disposable/dispose! graphics)
  (disposable/dispose! vis-ui)
  (disposable/dispose! world))
