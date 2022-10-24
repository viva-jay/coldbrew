import http from 'k6/http';
import { check } from "k6";

export let options = {
    duration: '1s',
    vus: 1,

    thresholds: {
        "http_req_duration": [`p(95) < ${__ENV.REQ_DURATION_THRESHOLDS}`],
    },
};

export default function () {
    let response = http.get("http://www.google.com");
    let length = response.body.length
    check(length, { "body length ": (v) => v >= 0});
};
