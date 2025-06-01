(ns cdq.levelgen
  (:require [cdq.create.assets]
            [cdq.create.db]
            [cdq.level.modules :as modules])
  (:import (com.badlogic.gdx.utils Disposable)))

(defrecord Context [])

(def state (atom nil))

(defn create! [config]
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config {:db {:schemas "schema.edn"
                                         :properties "properties.edn"}
                                    :assets {:folder "resources/"
                                             :asset-type-extensions {:texture #{"png" "bmp"}}}})
        ctx (cdq.create.db/do!     ctx)
        ctx (cdq.create.assets/do! ctx)
        level (modules/create ctx)]
    (reset! state ctx)
    (println level)))

(defn dispose! []
  (let [{:keys [ctx/assets]} @state]
    (Disposable/.dispose assets)))

(defn render! [])

(defn resize! [width height])
