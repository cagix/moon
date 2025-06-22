(ns cdq.application.context
  (:require [qrecord.core :as q]))

(q/defrecord Context [ctx/app
                      ctx/files
                      ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(defn create []
  (map->Context {}))
