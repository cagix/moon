(ns cdq.render.stage)

(defn render [{:keys [cdq.context/stage] :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (com.badlogic.gdx.scenes.scene2d.Stage/.draw stage)
  (set! (.applicationState stage) context)
  (com.badlogic.gdx.scenes.scene2d.Stage/.act stage)
  context)
