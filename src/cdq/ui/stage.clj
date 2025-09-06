(ns cdq.ui.stage
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn add! [stage actor-or-decl]
  (stage/add! stage (ui/construct? actor-or-decl)))
