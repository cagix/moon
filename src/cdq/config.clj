(ns cdq.config
  (:require [cdq.utils]
            [clojure.walk :as walk]
            [gdl.utils])
  (:import (clojure.lang ILookup)))

(def log? false)

(defn- require-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (if (namespace form)
                       (do
                        (when log?
                          (println "requiring-resolve " form))
                        (requiring-resolve form))
                       (do
                        (when log?
                          (println "require " form))
                        (require form)))
                     form))
                 form))

(defn create [path]
  (let [m (require-symbols (cdq.utils/io-slurp-edn path))]
    (reify ILookup
      (valAt [_ k]
        (gdl.utils/safe-get m k)))))

(require 'gdl.application)

(require 'cdq.game-state)
(require 'cdq.game-state.create-actors)

(require 'cdq.render)
(require 'cdq.render.render-entities)

(extend gdl.application.Context
  cdq.game-state/StageActors
  {:create-actors cdq.game-state.create-actors/create-actors}
  cdq.render/Render
  {:render-entities! cdq.render.render-entities/render-entities!})

; TODO this doesn;t work when we reload gdl.application.Context
