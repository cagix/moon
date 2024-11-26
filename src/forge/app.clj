(ns forge.app)

(declare current-screen-key
         screens)

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (dispose [_]))

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (.bindRoot #'current-screen-key new-k)
    (enter screen)))

(defn stage []
  (:stage (current-screen)))
