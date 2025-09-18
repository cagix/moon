(ns cdq.application.create
  (:require [clojure.utils :as utils]))

(defn do! [state
           {:keys [clojure.gdx/audio
                   clojure.gdx/files
                   clojure.gdx/graphics
                   clojure.gdx/input]}
           pipeline]
  (reset! state (utils/pipeline
                 {:ctx/audio audio
                  :ctx/files files
                  :ctx/graphics graphics
                  :ctx/input input}
                 pipeline)))
