(ns cdq.editor.widget
  (:require [cdq.schema :as schema]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(declare k->methods)

(defn create [schema attribute v ctx]
  (if-let [f (:create (k->methods (schema/widget-type schema attribute)))]
    (f schema attribute v ctx)
    ((:create (k->methods :default)) schema attribute v ctx)))

(defn value [schema attribute widget schemas]
  (if-let [f (:value (k->methods (schema/widget-type schema attribute)))]
    (f schema attribute widget schemas)
    ((:value (k->methods :default)) schema attribute widget schemas)))

(defn build [ctx schema k v]
  (let [widget (actor/build? (create schema k v ctx))]
    (actor/set-user-object! widget [k v])
    widget))
