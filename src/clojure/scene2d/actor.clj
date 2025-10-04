(ns clojure.scene2d.actor
  (:require [gdl.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [gdl.scene2d.stage :as stage]))

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn act!
  "If actor is not part of a stage, returns nil.
  Otherwise applies the function `f` to `(f actor delta ctx)`.
  Use in actor implementations to have a context fuuzubazol."
  [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))
