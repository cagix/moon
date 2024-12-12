(ns gdl.input
  (:require [clojure.gdx :as gdx]
            [gdl.utils :refer [gdx-static-field]]))

(def ^:private k->input-button (partial gdx-static-field "Input$Buttons"))
(def ^:private k->input-key    (partial gdx-static-field "Input$Keys"))

(defn button-just-pressed? [b]
  (gdx/button-just-pressed? (k->input-button b)))

(defn key-just-pressed? [k]
  (gdx/key-just-pressed? (k->input-key k)))

(defn key-pressed? [k]
  (gdx/key-pressed? (k->input-key k)))
