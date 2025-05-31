(ns cdq.create.stage
  (:require [cdq.g :as g]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private -k :ctx/stage)

(defn do! [{:keys [ctx/ui-viewport
                   ctx/batch]
            :as ctx}]
  (extend (class ctx)
    g/Stage
    {:add-actor! (fn [ctx actor]
                   (ui/add! (-k ctx) actor))
     :mouseover-actor (fn [ctx]
                        (ui/hit (-k ctx) (g/ui-mouse-position ctx)))})
  (let [stage (ui/stage (:java-object ui-viewport)
                        batch)]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx -k stage)))
