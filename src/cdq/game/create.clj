(ns cdq.game.create
  (:require [cdq.input :as input]
            [qrecord.core :as q]))

(q/defrecord Context [])

(defn do!
  [app config]
  (let [audio (.getAudio app)
        files (.getFiles app)
        graphics (.getGraphics app)
        input (.getInput app)
        graphics ((:graphics-impl config) graphics files (:graphics config))
        stage ((:ui-impl config) graphics (:ui-config config))
        ctx (-> (map->Context {})
                (assoc :ctx/graphics graphics)
                (assoc :ctx/stage stage)
                ((:handle-txs config))
                (assoc :ctx/audio ((:audio-impl config) audio files (:audio config)))
                (assoc :ctx/db ((:db-impl config)))
                (assoc :ctx/input input)
                (assoc :ctx/config {:world-impl (:world-impl config)}))]
    (input/set-processor! input stage)
    ((:add-actors config) stage ctx)
    ((:create-world config) ctx (:world config))))
