(ns cdq.ctx.dispose
  (:require [cdq.graphics.dispose :as dispose-graphics]
            [gdl.disposable :as disposable]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/vis-ui
                   ctx/world]}]
  (disposable/dispose! audio)
  (dispose-graphics/do! graphics)
  (disposable/dispose! vis-ui)
  (disposable/dispose! world))
