(ns clojure.gdx.scene2d.ctx-stage
  (:import (clojure.gdx.scene2d Stage)))

(defn create [viewport batch]
  (Stage. viewport batch (atom nil)))

(defn get-ctx [^Stage stage]
  @(.ctx stage))

(defn set-ctx! [^Stage stage ctx]
  (reset! (.ctx stage) ctx))
