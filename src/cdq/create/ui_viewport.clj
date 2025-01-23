(ns cdq.create.ui-viewport
  (:require [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(def config {:width 1440 :height 900})

(defn create [_context]
  (fit-viewport/create (:width  config)
                       (:height config)
                       (OrthographicCamera.)))
