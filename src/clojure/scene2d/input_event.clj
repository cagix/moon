(ns clojure.scene2d.input-event)

(defprotocol InputEvent
  (stage [event]))
