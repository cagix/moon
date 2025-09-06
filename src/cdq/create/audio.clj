(ns cdq.create.audio
  (:require [cdq.audio :as audio]
            [clojure.gdx :as gdx]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/audio (audio/create (gdx/audio) (gdx/files) (:audio config))))
