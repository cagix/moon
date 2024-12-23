(in-ns 'clojure.core)

(require '[clojure.gdx.graphics.color :as color]
         '[clojure.gdx.input :as input]
         '[clojure.gdx.utils.screen-utils :as screen-utils]
         '[gdl.ui.group :as group]
         '[gdl.ui.actor :as actor])

(defn clear-screen []
  (screen-utils/clear color/black))

(def key-just-pressed?    input/key-just-pressed?)
(def key-pressed?         input/key-pressed?)
(def button-just-pressed? input/button-just-pressed?)
(def set-input-processor  input/set-processor)

(def children group/children)

(def toggle-visible! actor/toggle-visible!)
(def visible?        actor/visible?)
(def set-visible     actor/set-visible)
