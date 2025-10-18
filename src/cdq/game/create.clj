(ns cdq.game.create
  (:require [cdq.input :as input]
            [qrecord.core :as q]))

(q/defrecord Context [])

(defn do!
  [{:keys [clojure.gdx/audio
           clojure.gdx/files
           clojure.gdx/graphics
           clojure.gdx/input]
    :as ctx}
   config]
  (let [graphics ((:graphics-impl config) graphics files (:graphics config))
        stage ((:ui-impl config) graphics (:ui-config config))
        ctx (-> (map->Context {})
                (assoc :ctx/graphics graphics)
                (assoc :ctx/stage stage)
                ((:handle-txs config))
                (assoc :ctx/audio ((:audio-impl config) audio files (:audio config)))
                (assoc :ctx/db ((:db-impl config)))
                (assoc :ctx/input input))]
    (input/set-processor! input stage)
    ((:add-actors config) stage ctx)
    ((:create-world config) ctx (:world config))))
