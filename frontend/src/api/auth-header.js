export default function authHeader() {
  let user = JSON.parse(localStorage.getItem('user'));

  if (user && user.accessToken) {
    // For Spring Boot back-end
    return { Authorization: 'Bearer ' + user.accessToken };
  } else {
    return {};
  }
}
