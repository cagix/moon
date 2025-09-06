(ns cdq.ui.stage
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage])
  (:import (clojure.lang ILookup)
           (clojure.gdx.scenes.scene2d Stage)))

(defn create [viewport batch]
  (proxy [Stage ILookup] [viewport batch (atom nil)]
    (valAt [id]
      (group/find-actor-with-id (stage/root this) id))))

(defn get-ctx [^Stage stage]
  @(.ctx ^Stage stage))

(defn set-ctx! [^Stage stage ctx]
  (reset! (.ctx ^Stage stage) ctx))

(defn add! [stage actor-or-decl]
  (stage/add! stage (actor/construct? actor-or-decl)))

(def act!   stage/act!)
(def draw!  stage/draw!)
(def clear! stage/clear!)
(def root   stage/root)
(def hit    stage/hit)
