(in-ns 'clojure.core)

(require '[clojure.gdx :as gdx]
         '[clojure.gdx.graphics.color :as color]
         '[clojure.gdx.input :as input]
         '[clojure.gdx.utils.screen-utils :as screen-utils]
         '[gdl.ui.group :as group]
         '[gdl.ui.actor :as actor])

(defn clear-screen []
  (screen-utils/clear color/black))

(defn key-just-pressed? [k]
  (input/key-just-pressed? gdx/input k))

(defn key-pressed? [k]
  (input/key-pressed? gdx/input k))

(defn button-just-pressed? [b]
  (input/button-just-pressed? gdx/input b))

(def children group/children)

(def toggle-visible! actor/toggle-visible!)
(def visible?        actor/visible?)
(def set-visible     actor/set-visible)
