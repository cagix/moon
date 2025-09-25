(ns gdl.grid2d-test)

; 2dimvector is 7x faster than a hashmap of [x y] to values
; like in rich hickey ant demo vectors of vectors:
; https://github.com/juliangamble/clojure-ants-simulation/blob/master/src/ants.clj

; Could put width and height as deftype arguments instead of protocol functions
(comment (let [g (create-grid 5 5 identity)]
           (compare-times 100000
                          (.width ^VectorGrid g) ; 1.575   ms
                          (.width g)             ; 305.477 ms
                          (width g))))           ; 2.761   ms

; getting multiple cells could be speed up
; if multiple posis in same row => only 1 row access

;Could also use a dogrid or docells function instead of doseq [p cell] -> could be a lot faster
;Time of:  (docells grid myfunc)
;"Elapsed time: 2.455 msecs"
;Time of:  (doseq [[p cell] grid] (myfunc p cell))
;"Elapsed time: 6.649 msecs"
;Time of:  (doseq [cell (cells grid) p (posis grid)] (myfunc p cell))
;"Elapsed time: 442.595 msecs"

; nth is faster than get: and get-in is very slow:
; (let [v (mapv (fn [i] :foo) (range 10000))] (time (dotimes [_ 1e8] (nth v 657))))
; "Elapsed time: 771.518 msecs"
; (let [v (mapv (fn [i] :foo) (range 10000))] (time (dotimes [_ 1e8] (get v 657))))
; "Elapsed time: 1671.101 msecs"
;
; user=> (time (dotimes [_ 1e8] (-> agrid (nth 10) (nth 20))))
; "Elapsed time: 1670.374 msecs"
; user=> (time (dotimes [_ 1e8] (get-in agrid [10 20])))
; "Elapsed time: 15219.404 msecs"
; user=>  (time  (dotimes  [_ 1e8]  (-> agrid  (get 10)  (get 20))))
; "Elapsed time: 2909.678 msecs"
; user=> (time (dotimes [_ 1e8] (.nth ^Indexed (.nth ^Indexed agrid 10) 20)))
; "Elapsed time: 1637.728 msecs"
;
(comment
  (let [data (.data (create-grid 100 100 identity))
        p [23 43]
        n 1e7]
    (time (dotimes [_ n]
            (-> data
                (nth (p 0) nil)
                (nth (p 1) nil))))
    (time (dotimes [_ n]
            (-> data
                (nth (nth p 0) nil)
                (nth (nth p 1) nil))))))
; 250ms
; 230ms

(comment
  (let [g (create-grid 100 100 identity)
        n 1e8]
    (time (dotimes [_ n]     (g [10 13])))
    (time (dotimes [_ n] (get g [10 13])))
    (time (dotimes [_ n] (.valAt ^clojure.lang.ILookup g [10 13])))))
;"Elapsed time: 2440.053 msecs" -> IFn -> .valAt
;"Elapsed time: 2964.275 msecs" -> clojure.core/get -> clojure.lang.RT/get -> .valAt
;"Elapsed time: 2438.005 msecs" -> .valAt

;(defn- vector1d [width height xyfn]
;  (mapv (fn [i]
;          (let [x (mod i width)
;                y (int (/ i width)) ]
;            (xyfn [x y])))
;        (range (* width height))))
;
;(let [width 100,height 100
;      g2d (vector2d width height identity)
;      g1d (vector1d width height identity)
;      x 10,y 13
;      n 1e7]
;  (time (dotimes [_ n] (-> g2d (nth x nil) (nth y nil)))) ; 439ms
;  (time (dotimes [_ n] (nth g1d (+ x (* width y)))))      ; 587ms
;  )
